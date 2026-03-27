package gg.agit.konect.domain.notification.service;

import static gg.agit.konect.global.code.ApiResponseCode.INVALID_NOTIFICATION_TOKEN;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.service.ChatPresenceService;
import gg.agit.konect.domain.notification.dto.NotificationInboxResponse;
import gg.agit.konect.domain.notification.dto.NotificationTokenDeleteRequest;
import gg.agit.konect.domain.notification.dto.NotificationTokenRegisterRequest;
import gg.agit.konect.domain.notification.dto.NotificationTokenResponse;
import gg.agit.konect.domain.notification.enums.NotificationInboxType;
import gg.agit.konect.domain.notification.enums.NotificationTargetType;
import gg.agit.konect.domain.notification.model.NotificationDeviceToken;
import gg.agit.konect.domain.notification.model.NotificationInbox;
import gg.agit.konect.domain.notification.repository.NotificationDeviceTokenRepository;
import gg.agit.konect.domain.notification.repository.NotificationMuteSettingRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    private static final Pattern EXPO_PUSH_TOKEN_PATTERN =
        Pattern.compile("^(ExponentPushToken|ExpoPushToken)\\[[^\\]]+\\]$");
    private static final String DEFAULT_NOTIFICATION_CHANNEL_ID = "default_notifications";
    private static final int CHAT_MESSAGE_PREVIEW_MAX_LENGTH = 30;
    private static final String CHAT_MESSAGE_PREVIEW_SUFFIX = "...";

    private final UserRepository userRepository;
    private final NotificationDeviceTokenRepository notificationDeviceTokenRepository;
    private final NotificationMuteSettingRepository notificationMuteSettingRepository;
    private final ChatPresenceService chatPresenceService;
    private final ExpoPushClient expoPushClient;
    private final NotificationInboxService notificationInboxService;

    public NotificationTokenResponse getMyToken(Integer userId) {
        NotificationDeviceToken token = notificationDeviceTokenRepository.getByUserId(userId);

        return NotificationTokenResponse.from(token);
    }

    @Transactional
    public void registerToken(Integer userId, NotificationTokenRegisterRequest request) {
        User user = userRepository.getById(userId);
        String token = request.token();
        validateExpoToken(token);

        notificationDeviceTokenRepository.findByUserId(userId)
            .ifPresentOrElse(
                notificationDeviceToken -> notificationDeviceToken.updateToken(token),
                () -> notificationDeviceTokenRepository.save(NotificationDeviceToken.of(user, token))
            );
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
                    NotificationTargetType.CHAT_ROOM,
                    roomId,
                    receiverId
                )
                .map(setting -> Boolean.TRUE.equals(setting.getIsMuted()))
                .orElse(false);

            if (isMuted) {
                log.debug("Direct chat muted, skipping notification: roomId={}, receiverId={}", roomId, receiverId);
                return;
            }

            String truncatedBody = buildPreview(messageContent);
            String path = "chats/" + roomId;

            NotificationInbox saved = notificationInboxService.save(
                receiverId,
                NotificationInboxType.CHAT_MESSAGE,
                senderName,
                truncatedBody,
                path
            );

            notificationInboxService.sendSse(receiverId, NotificationInboxResponse.from(saved));

            List<String> tokens = notificationDeviceTokenRepository.findTokensByUserId(receiverId);
            if (tokens.isEmpty()) {
                log.debug("No device tokens found for user: receiverId={}", receiverId);
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("path", path);

            expoPushClient.sendNotification(receiverId, tokens, senderName, truncatedBody, data);
        } catch (Exception e) {
            log.error("Failed to send chat notification: roomId={}, receiverId={}", roomId, receiverId, e);
        }
    }

    @Async
    public void sendGroupChatNotification(
        Integer roomId,
        Integer senderId,
        String clubName,
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

            // 채팅방에 접속하고 있는 유저 목록
            Set<Integer> activeUsers = chatPresenceService.findUsersInChatRoom(roomId, filteredRecipients);

            // 채팅방 알림을 뮤트 처리한 유저 목록
            Set<Integer> mutedUsers = notificationMuteSettingRepository
                .findMutedUserIdsByTargetTypeAndTargetIdAndUserIds(
                    NotificationTargetType.CHAT_ROOM, roomId, filteredRecipients);

            List<Integer> targetRecipients = filteredRecipients.stream()
                .filter(id -> !activeUsers.contains(id))
                .filter(id -> !mutedUsers.contains(id))
                .toList();

            if (targetRecipients.isEmpty()) {
                log.info(
                    "Group chat notification completed: roomId={}, totalRecipients={}, active={}, muted={}, target=0",
                    roomId,
                    filteredRecipients.size(),
                    activeUsers.size(),
                    mutedUsers.size()
                );
                return;
            }

            String truncatedBody = buildPreview(messageContent);
            String previewBody = senderName + ": " + truncatedBody;
            String path = "chats/" + roomId;

            List<NotificationInbox> savedInboxes = notificationInboxService.saveAll(
                targetRecipients,
                NotificationInboxType.GROUP_CHAT_MESSAGE,
                clubName,
                previewBody,
                path
            );

            notificationInboxService.sendSseBatch(targetRecipients, savedInboxes);

            List<String> tokens = notificationDeviceTokenRepository.findTokensByUserIds(targetRecipients);

            Map<String, Object> data = new HashMap<>();
            data.put("path", path);

            List<ExpoPushClient.ExpoPushMessage> messages = tokens.stream()
                .map(token -> new ExpoPushClient.ExpoPushMessage(
                    token, clubName, previewBody, data, DEFAULT_NOTIFICATION_CHANNEL_ID))
                .toList();

            if (!messages.isEmpty()) {
                expoPushClient.sendBatchNotifications(messages);
            }

            log.info(
                "Group chat notification completed: roomId={}, total={}, active={}, muted={}, target={}, tokens={}",
                roomId,
                filteredRecipients.size(),
                activeUsers.size(),
                mutedUsers.size(),
                targetRecipients.size(),
                messages.size()
            );
        } catch (Exception e) {
            log.error("Failed to send group chat notification: roomId={}, senderId={}", roomId, senderId, e);
        }
    }

    @Async
    public void sendClubApplicationSubmittedNotification(
        Integer receiverId,
        Integer applicationId,
        Integer clubId,
        String clubName,
        String applicantName
    ) {
        String body = applicantName + "님이 동아리 가입을 신청했어요.";
        String path = "mypage/manager/" + clubId + "/applications/" + applicationId;
        NotificationInbox saved = notificationInboxService.save(
            receiverId, NotificationInboxType.CLUB_APPLICATION_SUBMITTED, clubName, body, path);
        notificationInboxService.sendSse(receiverId, NotificationInboxResponse.from(saved));
        sendNotification(receiverId, clubName, body, path);
    }

    @Async
    public void sendClubApplicationApprovedNotification(Integer receiverId, Integer clubId, String clubName) {
        String body = "동아리 지원이 승인되었어요.";
        String path = "clubs/" + clubId;
        NotificationInbox saved = notificationInboxService.save(
            receiverId, NotificationInboxType.CLUB_APPLICATION_APPROVED, clubName, body, path);
        notificationInboxService.sendSse(receiverId, NotificationInboxResponse.from(saved));
        sendNotification(receiverId, clubName, body, path);
    }

    @Async
    public void sendClubApplicationRejectedNotification(Integer receiverId, Integer clubId, String clubName) {
        String body = "동아리 지원이 거절되었어요.";
        String path = "clubs/" + clubId;
        NotificationInbox saved = notificationInboxService.save(
            receiverId, NotificationInboxType.CLUB_APPLICATION_REJECTED, clubName, body, path);
        notificationInboxService.sendSse(receiverId, NotificationInboxResponse.from(saved));
        sendNotification(receiverId, clubName, body, path);
    }

    private void sendNotification(Integer receiverId, String title, String body, String path) {
        List<String> tokens = notificationDeviceTokenRepository.findTokensByUserId(receiverId);
        if (tokens.isEmpty()) {
            log.debug("No device tokens found for user: receiverId={}", receiverId);
            return;
        }

        Map<String, Object> data = buildData(null, path);
        expoPushClient.sendNotification(receiverId, tokens, title, body, data);
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

    private void validateExpoToken(String token) {
        if (!EXPO_PUSH_TOKEN_PATTERN.matcher(token).matches()) {
            throw CustomException.of(INVALID_NOTIFICATION_TOKEN);
        }
    }

    private record ExpoPushTicket(String status, String message, Map<String, Object> details) {
    }
}
