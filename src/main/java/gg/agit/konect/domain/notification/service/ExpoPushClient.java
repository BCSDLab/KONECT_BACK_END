package gg.agit.konect.domain.notification.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExpoPushClient {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    private static final String DEFAULT_NOTIFICATION_CHANNEL_ID = "default_notifications";

    private final RestTemplate expoRestTemplate;

    public ExpoPushClient(@Qualifier("expoRestTemplate") RestTemplate expoRestTemplate) {
        this.expoRestTemplate = expoRestTemplate;
    }

    @Retryable(maxAttempts = 2)
    public void sendNotification(Integer receiverId, List<String> tokens, String title, String body, Map<String, Object> data) {
        List<ExpoPushMessage> messages = tokens.stream()
            .map(token -> new ExpoPushMessage(token, title, body, data, DEFAULT_NOTIFICATION_CHANNEL_ID))
            .toList();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<List<ExpoPushMessage>> entity = new HttpEntity<>(messages, headers);
        ResponseEntity<ExpoPushResponse> response = expoRestTemplate.exchange(
            EXPO_PUSH_URL,
            HttpMethod.POST,
            entity,
            ExpoPushResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException(
                "Expo push response not successful: receiverId=%d, status=%s"
                    .formatted(receiverId, response.getStatusCode())
            );
        }

        ExpoPushResponse responseBody = response.getBody();
        if (responseBody == null || responseBody.data() == null) {
            throw new IllegalStateException(
                "Expo push response body missing: receiverId=%d".formatted(receiverId)
            );
        }

        for (int i = 0; i < responseBody.data().size(); i += 1) {
            ExpoPushTicket ticket = responseBody.data().get(i);
            if (ticket == null || "ok".equalsIgnoreCase(ticket.status())) {
                continue;
            }
            String token = i < tokens.size() ? tokens.get(i) : "unknown";
            log.error(
                "Expo push failed: receiverId={}, token={}, status={}, message={}, details={}",
                receiverId,
                token,
                ticket.status(),
                ticket.message(),
                ticket.details()
            );
        }

        log.debug("Notification sent: receiverId={}, tokenCount={}", receiverId, tokens.size());
    }

    @Recover
    public void sendNotificationRecover(Exception e, Integer receiverId, List<String> tokens, String title, String body,
        Map<String, Object> data) {
        log.error("Failed to send notification after retry: receiverId={}, tokenCount={}", receiverId, tokens.size(), e);
    }

    private record ExpoPushMessage(String to, String title, String body, Map<String, Object> data, String channelId) {
    }

    private record ExpoPushResponse(List<ExpoPushTicket> data) {
    }

    private record ExpoPushTicket(String status, String message, Map<String, Object> details) {
    }
}
