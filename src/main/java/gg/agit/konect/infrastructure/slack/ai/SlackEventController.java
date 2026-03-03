package gg.agit.konect.infrastructure.slack.ai;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.infrastructure.slack.config.SlackSignatureVerifier;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Hidden
@RestController
@RequestMapping("/slack/events")
@RequiredArgsConstructor
public class SlackEventController {

    private static final String SLACK_TIMESTAMP_HEADER = "X-Slack-Request-Timestamp";
    private static final String SLACK_SIGNATURE_HEADER = "X-Slack-Signature";

    private final SlackAIService slackAIService;
    private final SlackSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;

    @PostMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<Object> handleSlackEvent(
        @RequestHeader(value = SLACK_TIMESTAMP_HEADER, required = false) String timestamp,
        @RequestHeader(value = SLACK_SIGNATURE_HEADER, required = false) String signature,
        @RequestBody String rawBody
    ) {
        Map<String, Object> payload = parsePayload(rawBody);
        if (payload == null) {
            log.warn("Slack 요청 본문 파싱 실패");
            return ResponseEntity.badRequest().build();
        }

        String type = (String)payload.get("type");

        // URL 검증은 서명 검증 없이 처리 (최초 설정 시)
        if ("url_verification".equals(type)) {
            String challenge = (String)payload.get("challenge");
            log.info("Slack URL 검증 요청 처리");
            return ResponseEntity.ok(Map.of("challenge", challenge));
        }

        // 서명 검증 - 원본 요청 본문 사용
        if (!signatureVerifier.isValidRequest(timestamp, signature, rawBody)) {
            log.warn("Slack 서명 검증 실패");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.debug("Slack 이벤트 수신: type={}", type);

        // 이벤트 콜백 처리
        if ("event_callback".equals(type)) {
            Map<String, Object> event = (Map<String, Object>)payload.get("event");
            if (event != null) {
                handleEvent(event);
            }
        }

        // Slack은 3초 내 응답을 기대하므로 빠르게 200 반환
        return ResponseEntity.ok().build();
    }

    private Map<String, Object> parsePayload(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패", e);
            return null;
        }
    }

    private void handleEvent(Map<String, Object> event) {
        String eventType = (String)event.get("type");
        String text = (String)event.get("text");
        String subtype = (String)event.get("subtype");

        log.debug("이벤트 처리: eventType={}", eventType);

        // bot 메시지나 변경 이벤트는 무시
        if (subtype != null) {
            return;
        }

        // 메시지 이벤트 처리
        if ("message".equals(eventType) && text != null) {
            if (slackAIService.isAIQuery(text)) {
                log.debug("AI 질문 감지");
                slackAIService.processAIQuery(text);
            }
        }

        // 앱 멘션 이벤트 처리
        if ("app_mention".equals(eventType) && text != null) {
            String normalizedText = slackAIService.normalizeAppMentionText(text);
            log.debug("앱 멘션 감지");
            slackAIService.processAIQuery(normalizedText);
        }
    }
}
