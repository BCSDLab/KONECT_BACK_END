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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
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
                "Expo 푸시 발송 실패: receiverId={}, token={}, status={}, message={}, details={}",
                receiverId,
                token,
                ticket.status(),
                ticket.message(),
                ticket.details()
            );
        }

        log.debug("알림 발송 완료: receiverId={}, tokenCount={}", receiverId, tokens.size());
    }

    @Recover
    public void sendNotificationRecover(HttpStatusCodeException e, Integer receiverId, List<String> tokens, String title,
        String body,
        Map<String, Object> data) {
        log.error(
            "알림 재시도 후에도 HTTP 오류로 발송에 실패했습니다: receiverId={}, tokenCount={}, statusCode={}, responseBody={}",
            receiverId,
            tokens.size(),
            e.getStatusCode(),
            e.getResponseBodyAsString(),
            e
        );
    }

    @Recover
    public void sendNotificationRecover(ResourceAccessException e, Integer receiverId, List<String> tokens, String title,
        String body,
        Map<String, Object> data) {
        Throwable rootCause = e.getMostSpecificCause();
        log.error(
            "알림 재시도 후에도 연결 문제로 발송에 실패했습니다: receiverId={}, tokenCount={}, rootCauseType={}, rootCauseMessage={}",
            receiverId,
            tokens.size(),
            rootCause.getClass().getSimpleName(),
            rootCause.getMessage(),
            e
        );
    }

    @Recover
    public void sendNotificationRecover(IllegalStateException e, Integer receiverId, List<String> tokens, String title,
        String body,
        Map<String, Object> data) {
        log.error(
            "알림 재시도 후에도 Expo 응답이 비정상이라 발송에 실패했습니다: receiverId={}, tokenCount={}, message={}",
            receiverId,
            tokens.size(),
            e.getMessage(),
            e
        );
    }

    @Recover
    public void sendNotificationRecover(RestClientException e, Integer receiverId, List<String> tokens, String title,
        String body,
        Map<String, Object> data) {
        log.error(
            "알림 재시도 후에도 Rest 클라이언트 오류로 발송에 실패했습니다: receiverId={}, tokenCount={}, exceptionType={}, message={}",
            receiverId,
            tokens.size(),
            e.getClass().getSimpleName(),
            e.getMessage(),
            e
        );
    }

    @Recover
    public void sendNotificationRecover(Exception e, Integer receiverId, List<String> tokens, String title, String body,
        Map<String, Object> data) {
        log.error(
            "알림 재시도 후에도 예기치 못한 오류로 발송에 실패했습니다: receiverId={}, tokenCount={}, exceptionType={}, message={}",
            receiverId,
            tokens.size(),
            e.getClass().getSimpleName(),
            e.getMessage(),
            e
        );
    }

    private record ExpoPushMessage(String to, String title, String body, Map<String, Object> data, String channelId) {
    }

    private record ExpoPushResponse(List<ExpoPushTicket> data) {
    }

    private record ExpoPushTicket(String status, String message, Map<String, Object> details) {
    }
}
