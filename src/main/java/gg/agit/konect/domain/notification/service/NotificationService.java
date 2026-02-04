package gg.agit.konect.domain.notification.service;

import static gg.agit.konect.global.code.ApiResponseCode.DUPLICATE_NOTIFICATION_TOKEN;
import static gg.agit.konect.global.code.ApiResponseCode.FAILED_SEND_NOTIFICATION;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import gg.agit.konect.domain.notification.dto.NotificationSendRequest;
import gg.agit.konect.domain.notification.dto.NotificationTokenDeleteRequest;
import gg.agit.konect.domain.notification.dto.NotificationTokenRegisterRequest;
import gg.agit.konect.domain.notification.model.NotificationDeviceToken;
import gg.agit.konect.domain.notification.repository.NotificationDeviceTokenRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    private static final Pattern EXPO_PUSH_TOKEN_PATTERN =
        Pattern.compile("^(ExponentPushToken|ExpoPushToken)\\[[^\\]]+\\]$");

    private final UserRepository userRepository;
    private final NotificationDeviceTokenRepository notificationDeviceTokenRepository;
    private final RestTemplate restTemplate;

    public NotificationService(
        UserRepository userRepository,
        NotificationDeviceTokenRepository notificationDeviceTokenRepository,
        RestTemplate restTemplate
    ) {
        this.userRepository = userRepository;
        this.notificationDeviceTokenRepository = notificationDeviceTokenRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public void registerToken(Integer userId, NotificationTokenRegisterRequest request) {
        User user = userRepository.getById(userId);
        String token = request.token();
        validateExpoToken(token);

        try {
            notificationDeviceTokenRepository.save(NotificationDeviceToken.of(user, token));
        } catch (DataIntegrityViolationException e) {
            Integer ownerId = notificationDeviceTokenRepository.findUserIdByToken(token)
                .orElseThrow(() -> e);

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

    public void sendToMe(Integer userId, NotificationSendRequest request) {
        List<String> tokens = notificationDeviceTokenRepository.findTokensByUserId(userId);

        if (tokens.isEmpty()) {
            return;
        }

        Map<String, Object> data = buildData(request.data(), request.path());
        List<ExpoPushMessage> messages = tokens.stream()
            .map(token -> new ExpoPushMessage(token, request.title(), request.body(), data))
            .toList();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<List<ExpoPushMessage>> entity = new HttpEntity<>(messages, headers);
        try {
            ResponseEntity<ExpoPushResponse> response = restTemplate.exchange(
                EXPO_PUSH_URL,
                HttpMethod.POST,
                entity,
                ExpoPushResponse.class
            );

            ExpoPushResponse body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null || body.hasError()) {
                throw CustomException.of(FAILED_SEND_NOTIFICATION);
            }
        } catch (RestClientException exception) {
            throw CustomException.of(FAILED_SEND_NOTIFICATION);
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
        return payload.isEmpty() ? null : payload;
    }

    private record ExpoPushMessage(String to, String title, String body, Map<String, Object> data) {
    }

    private record ExpoPushResponse(List<ExpoPushTicket> data) {
        boolean hasError() {
            if (data == null) {
                return true;
            }
            return data.stream().anyMatch(ticket -> !"ok".equalsIgnoreCase(ticket.status()));
        }
    }

    private void validateExpoToken(String token) {
        if (!EXPO_PUSH_TOKEN_PATTERN.matcher(token).matches()) {
            throw CustomException.of(INVALID_NOTIFICATION_TOKEN);
        }
    }

    private record ExpoPushTicket(String status, String message, Map<String, Object> details) {
    }
}
