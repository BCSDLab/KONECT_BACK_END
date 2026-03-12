package gg.agit.konect.infrastructure.slack.client;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.infrastructure.slack.config.SlackProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackClient {

    private static final String SLACK_API_BASE = "https://slack.com/api";

    private final RestTemplate restTemplate;
    private final SlackProperties slackProperties;
    private final ObjectMapper objectMapper;

    @Retryable
    public void sendMessage(String message, String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("text", message);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        restTemplate.postForEntity(url, request, String.class);
    }

    @Recover
    public void sendMessageRecover(Exception e, String message, String url) {
        log.error("Slack 메시지 전송 실패 : message={}, url={}", message, url, e);
    }

    @Retryable
    public void postThreadReply(String channelId, String threadTs, String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.setBearerAuth(slackProperties.botToken());

        Map<String, Object> payload = new HashMap<>();
        payload.put("channel", channelId);
        payload.put("thread_ts", threadTs);
        payload.put("text", text);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
            SLACK_API_BASE + "/chat.postMessage", request, String.class
        );

        Map<String, Object> parsed = parseSlackResponse(response.getBody());
        Boolean ok = (Boolean)parsed.get("ok");
        if (!Boolean.TRUE.equals(ok)) {
            String error = (String)parsed.get("error");
            log.error("Slack 스레드 응답 전송 실패: channelId={}, threadTs={}, error={}",
                channelId, threadTs, error);
        }
    }

    @Recover
    public void postThreadReplyRecover(Exception e, String channelId,
            String threadTs, String text) {
        log.error("Slack 스레드 응답 전송 최종 실패: channelId={}, threadTs={}", channelId, threadTs, e);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getThreadReplies(String channelId, String threadTs) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(slackProperties.botToken());

        HttpEntity<Void> request = new HttpEntity<>(headers);
        String url = SLACK_API_BASE + "/conversations.replies?channel=" + channelId
            + "&ts=" + threadTs;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, org.springframework.http.HttpMethod.GET, request, String.class
            );
            Map<String, Object> parsed = parseSlackResponse(response.getBody());
            Boolean ok = (Boolean)parsed.get("ok");
            if (!Boolean.TRUE.equals(ok)) {
                String error = (String)parsed.get("error");
                log.error("스레드 이력 조회 실패 (Slack API): channelId={}, threadTs={}, error={}",
                    channelId, threadTs, error);
                return new ArrayList<>();
            }
            Object messages = parsed.get("messages");
            if (messages instanceof List) {
                return (List<Map<String, Object>>)messages;
            }
        } catch (Exception e) {
            log.error("스레드 이력 조회 실패: channelId={}, threadTs={}", channelId, threadTs, e);
        }
        return new ArrayList<>();
    }

    private Map<String, Object> parseSlackResponse(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            log.error("Slack 응답 파싱 실패: {}", body, e);
            return new HashMap<>();
        }
    }
}
