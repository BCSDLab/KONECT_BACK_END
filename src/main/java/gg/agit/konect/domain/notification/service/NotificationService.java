package gg.agit.konect.domain.notification.service;

import static gg.agit.konect.global.code.ApiResponseCode.DUPLICATE_NOTIFICATION_TOKEN;
import static gg.agit.konect.global.code.ApiResponseCode.INVALID_NOTIFICATION_TOKEN;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import gg.agit.konect.domain.chat.unified.service.ChatPresenceService;
import gg.agit.konect.domain.notification.dto.NotificationTokenDeleteRequest;
import gg.agit.konect.domain.notification.dto.NotificationTokenRegisterRequest;
import gg.agit.konect.domain.notification.enums.NotificationTargetType;
import gg.agit.konect.domain.notification.model.NotificationDeviceToken;
import gg.agit.konect.domain.notification.repository.NotificationMuteSettingRepository;
import gg.agit.konect.domain.notification.repository.NotificationDeviceTokenRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(readOnly = true)
public class NotificationService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    private static final Pattern EXPO_PUSH_TOKEN_PATTERN =
        Pattern.compile("^(ExponentPushToken|ExpoPushToken)\\[[^\\]]+\\]$");
    private static final int CHAT_MESSAGE_PREVIEW_MAX_LENGTH = 30;
    private static final String CHAT_MESSAGE_PREVIEW_SUFFIX = "...";

    private final UserRepository userRepository;
    private final NotificationDeviceTokenRepository notificationDeviceTokenRepository;
    private final NotificationMuteSettingRepository notificationMuteSettingRepository;
    private final RestTemplate restTemplate;
    private final ChatPresenceService chatPresenceService;

    public NotificationService(
        UserRepository userRepository,
        NotificationDeviceTokenRepository notificationDeviceTokenRepository,
        NotificationMuteSettingRepository notificationMuteSettingRepository,
        RestTemplate restTemplate,
        ChatPresenceService chatPresenceService
    ) {
        this.userRepository = userRepository;
        this.notificationDeviceTokenRepository = notificationDeviceTokenRepository;
        this.notificationMuteSettingRepository = notificationMuteSettingRepository;
        this.restTemplate = restTemplate;
        this.chatPresenceService = chatPresenceService;
    }

    @Transactional
    public void registerToken(Integer userId, NotificationTokenRegisterRequest request) {
        User user = userRepository.getById(userId);
        String token = request.token();
        validateExpoToken(token);

        Integer existingOwnerId = notificationDeviceTokenRepository.findUserIdByToken(token)
            .orElse(null);
        if (existingOwnerId != null) {
            if (!existingOwnerId.equals(userId)) {
                throw CustomException.of(DUPLICATE_NOTIFICATION_TOKEN);
            }
            return;
        }

        try {
            notificationDeviceTokenRepository.save(NotificationDeviceToken.of(user, token));
        } catch (DataIntegrityViolationException e) {
            Integer ownerId = notificationDeviceTokenRepository.findUserIdByToken(token)
                .orElse(null);
            if (ownerId == null) {
                log.warn(
                    "Token uniqueness violation without owner: userId={}, token={}",
                    userId,
                    token
                );
                throw e;
            }
            if (!ownerId.equals(userId)) {
                throw CustomException.of(DUPLICATE_NOTIFICATION_TOKEN);
            }
        }
    }

    @Transactional
    public void deleteToken(Integer userId, NotificationTokenDeleteRequest request) {
        notificationDeviceTokenRepository.findByUserIdAndToken(userId, request.token())
            .ifPresent(notificationDeviceTokenRepository::delete);
    }

    @Async
    public void sendChatNotification(Integer receiverId, Integer roomId, String senderName, String messageContent) {
        try {
            if (chatPresenceService.isUserInChatRoom(roomId, receiverId)) {
                log.debug("User in chat room, skipping notification: roomId={}, receiverId={}", roomId, receiverId);
                return;
            }

            boolean isMuted = notificationMuteSettingRepository.findByTargetTypeAndTargetIdAndUserId(
                    NotificationTargetType.DIRECT_CHAT_ROOM,
                    roomId,
                    receiverId
                )
                .map(setting -> Boolean.TRUE.equals(setting.getIsMuted()))
                .orElse(false);

            if (isMuted) {
                log.debug("Direct chat muted, skipping notification: roomId={}, receiverId={}", roomId, receiverId);
                return;
            }

            List<String> tokens = notificationDeviceTokenRepository.findTokensByUserId(receiverId);
            if (tokens.isEmpty()) {
                log.debug("No device tokens found for user: receiverId={}", receiverId);
                return;
            }

            String truncatedBody = buildPreview(messageContent);
            Map<String, Object> data = new HashMap<>();
            data.put("path", "chats/rooms/" + roomId + "?type=DIRECT");

            List<ExpoPushMessage> messages = tokens.stream()
                .map(token -> new ExpoPushMessage(token, senderName, truncatedBody, data))
                .toList();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<List<ExpoPushMessage>> entity = new HttpEntity<>(messages, headers);
            ResponseEntity<ExpoPushResponse> response = restTemplate.exchange(
                EXPO_PUSH_URL,
                HttpMethod.POST,
                entity,
                ExpoPushResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error(
                    "Expo push response not successful: roomId={}, receiverId={}, status={}",
                    roomId,
                    receiverId,
                    response.getStatusCode()
                );
                return;
            }

            ExpoPushResponse body = response.getBody();
            if (body == null || body.data == null) {
                log.error("Expo push response body missing: roomId={}, receiverId={}", roomId, receiverId);
                return;
            }

            for (int i = 0; i < body.data.size(); i += 1) {
                ExpoPushTicket ticket = body.data.get(i);
                if (ticket == null || "ok".equalsIgnoreCase(ticket.status())) {
                    continue;
                }
                String token = i < tokens.size() ? tokens.get(i) : "unknown";
                log.error(
                    "Expo push failed: roomId={}, receiverId={}, token={}, status={}, message={}, details={}",
                    roomId,
                    receiverId,
                    token,
                    ticket.status(),
                    ticket.message(),
                    ticket.details()
                );
            }

            log.debug(
                "Chat notification sent: roomId={}, receiverId={}, tokenCount={}",
                roomId,
                receiverId,
                tokens.size()
            );
        } catch (Exception e) {
            log.error("Failed to send chat notification: roomId={}, receiverId={}", roomId, receiverId, e);
        }
    }

    @Async
    public void sendGroupChatNotification(
        Integer roomId,
        Integer senderId,
        String senderName,
        String messageContent,
        List<Integer> recipientUserIds
    ) {
        try {
            // 발신자를 수신자 목록에서 제외
            List<Integer> filteredRecipients = recipientUserIds.stream()
                .filter(recipientId -> !recipientId.equals(senderId))
                .toList();

            if (filteredRecipients.isEmpty()) {
                log.debug("No recipients after filtering sender: roomId={}, senderId={}", roomId, senderId);
                return;
            }

            String truncatedBody = buildPreview(messageContent);
            Map<String, Object> data = new HashMap<>();
            data.put("path", "chats/rooms/" + roomId + "?type=GROUP");

            for (Integer recipientId : filteredRecipients) {
                try {
                    // 사용자가 현재 채팅방에 접속 중인 경우 알림 전송 생략
                    if (chatPresenceService.isUserInChatRoom(roomId, recipientId)) {
                        log.debug(
                            "User in group chat room, skipping notification: roomId={}, recipientId={}",
                            roomId,
                            recipientId
                        );
                        continue;
                    }

                    List<String> tokens = notificationDeviceTokenRepository.findTokensByUserId(recipientId);
                    if (tokens.isEmpty()) {
                        log.debug("No device tokens found for user: recipientId={}", recipientId);
                        continue;
                    }

                    List<ExpoPushMessage> messages = tokens.stream()
                        .map(token -> new ExpoPushMessage(token, senderName, truncatedBody, data))
                        .toList();

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

                    HttpEntity<List<ExpoPushMessage>> entity = new HttpEntity<>(messages, headers);
                    ResponseEntity<ExpoPushResponse> response = restTemplate.exchange(
                        EXPO_PUSH_URL,
                        HttpMethod.POST,
                        entity,
                        ExpoPushResponse.class
                    );

                    if (!response.getStatusCode().is2xxSuccessful()) {
                        log.error(
                            "Expo push response not successful: roomId={}, recipientId={}, status={}",
                            roomId,
                            recipientId,
                            response.getStatusCode()
                        );
                        continue;
                    }

                    ExpoPushResponse body = response.getBody();
                    if (body == null || body.data == null) {
                        log.error(
                            "Expo push response body missing: roomId={}, recipientId={}",
                            roomId,
                            recipientId
                        );
                        continue;
                    }

                    for (int i = 0; i < body.data.size(); i += 1) {
                        ExpoPushTicket ticket = body.data.get(i);
                        if (ticket == null || "ok".equalsIgnoreCase(ticket.status())) {
                            continue;
                        }
                        String token = i < tokens.size() ? tokens.get(i) : "unknown";
                        log.error(
                            "Expo push failed: roomId={}, recipientId={}, token={}, status={}, message={}, details={}",
                            roomId,
                            recipientId,
                            token,
                            ticket.status(),
                            ticket.message(),
                            ticket.details()
                        );
                    }

                    log.debug(
                        "Group chat notification sent: roomId={}, recipientId={}, tokenCount={}",
                        roomId,
                        recipientId,
                        tokens.size()
                    );
                } catch (Exception e) {
                    log.error(
                        "Failed to send group chat notification to recipient: roomId={}, recipientId={}",
                        roomId,
                        recipientId,
                        e
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed to send group chat notification: roomId={}, senderId={}", roomId, senderId, e);
        }
    }

    private Map<String, Object> buildData(Map<String, String> data, String path) {
        Map<String, Object> payload = new HashMap<>();
        if (data != null && !data.isEmpty()) {
            payload.putAll(data);
        }
        if (path != null && !path.isBlank()) {
            payload.put("path", path);
        }
        return payload;
    }

    private String buildPreview(String messageContent) {
        if (messageContent == null) {
            return "";
        }

        int codePointCount = messageContent.codePointCount(0, messageContent.length());
        if (codePointCount <= CHAT_MESSAGE_PREVIEW_MAX_LENGTH) {
            return messageContent;
        }

        int endIndex = messageContent.offsetByCodePoints(0, CHAT_MESSAGE_PREVIEW_MAX_LENGTH);
        return messageContent.substring(0, endIndex) + CHAT_MESSAGE_PREVIEW_SUFFIX;
    }

    private record ExpoPushMessage(String to, String title, String body, Map<String, Object> data) {
    }

    private record ExpoPushResponse(List<ExpoPushTicket> data) {
    }

    private void validateExpoToken(String token) {
        if (!EXPO_PUSH_TOKEN_PATTERN.matcher(token).matches()) {
            throw CustomException.of(INVALID_NOTIFICATION_TOKEN);
        }
    }

    private record ExpoPushTicket(String status, String message, Map<String, Object> details) {
    }
}
