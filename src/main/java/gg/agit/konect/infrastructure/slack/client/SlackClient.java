package gg.agit.konect.infrastructure.slack.client;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackClient {

    private final RestTemplate restTemplate;

    public void sendMessage(String message, String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(APPLICATION_JSON);

            Map<String, Object> payload = new HashMap<>();
            payload.put("text", message);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(
                url,
                request,
                String.class
            );
        } catch (Exception e) {
            log.error("Slack 메시지 전송 중 오류 발생", e);
        }
    }
}
