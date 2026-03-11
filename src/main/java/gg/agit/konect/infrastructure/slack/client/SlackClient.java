package gg.agit.konect.infrastructure.slack.client;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.infrastructure.slack.config.SlackProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackClient {

    private static final String CHAT_POST_MESSAGE_URL = "https://slack.com/api/chat.postMessage";
    private static final String CONVERSATIONS_REPLIES_URL = "https://slack.com/api/conversations.replies";

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

    public void postThreadReply(String channelId, String threadTs, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.setBearerAuth(slackProperties.botToken());

        Map<String, Object> payload = new HashMap<>();
        payload.put("channel", channelId);
        payload.put("thread_ts", threadTs);
        payload.put("text", message);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        try {
            restTemplate.postForEntity(CHAT_POST_MESSAGE_URL, request, String.class);
        } catch (Exception e) {
            log.error("Slack 스레드 답글 전송 실패: channel={}, threadTs={}", channelId, threadTs, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getThreadReplies(String channelId, String threadTs) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(slackProperties.botToken());

        String url = CONVERSATIONS_REPLIES_URL + "?channel=" + channelId + "&ts=" + threadTs;
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            String responseBody = restTemplate.exchange(url, HttpMethod.GET, request, String.class).getBody();

            if (responseBody == null) {
                return new ArrayList<>();
            }

            Map<String, Object> parsed = objectMapper.readValue(responseBody, new TypeReference<>() { });
            Object messages = parsed.get("messages");

            if (messages == null) {
                return new ArrayList<>();
            }

            return objectMapper.convertValue(messages, new TypeReference<>() { });
        } catch (Exception e) {
            log.error("Slack 스레드 이력 조회 실패: channel={}, threadTs={}", channelId, threadTs, e);
            return new ArrayList<>();
        }
    }
}
