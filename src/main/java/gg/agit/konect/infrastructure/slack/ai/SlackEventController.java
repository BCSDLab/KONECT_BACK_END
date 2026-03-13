package gg.agit.konect.infrastructure.slack.ai;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final int EVENT_CACHE_MAX_SIZE = 500;

    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

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
            log.warn("Slack \uc694\uccad \ubcf8\ubb38 \ud30c\uc2f1 \uc2e4\ud328");
            return ResponseEntity.badRequest().build();
        }

        String type = (String)payload.get("type");

        if ("url_verification".equals(type)) {
            String challenge = (String)payload.get("challenge");
            log.info("Slack URL \uac80\uc99d \uc694\uccad \ucc98\ub9ac");
            return ResponseEntity.ok(Map.of("challenge", challenge));
        }

        if (!signatureVerifier.isValidRequest(timestamp, signature, rawBody)) {
            log.warn("Slack \uc11c\uba85 \uac80\uc99d \uc2e4\ud328");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.debug("Slack \uc774\ubca4\ud2b8 \uc218\uc2e0: type={}", type);

        if ("event_callback".equals(type)) {
            String eventId = (String)payload.get("event_id");
            if (eventId != null && !processedEventIds.add(eventId)) {
                log.debug("\uc911\ubcf5 \uc774\ubca4\ud2b8 \ubb34\uc2dc: event_id={}", eventId);
                return ResponseEntity.ok().build();
            }
            if (processedEventIds.size() > EVENT_CACHE_MAX_SIZE) {
                processedEventIds.remove(processedEventIds.iterator().next());
            }
            Map<String, Object> event = (Map<String, Object>)payload.get("event");
            if (event != null) {
                handleEvent(event);
            }
        }

        return ResponseEntity.ok().build();
    }

    private Map<String, Object> parsePayload(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("JSON \ud30c\uc2f1 \uc2e4\ud328", e);
            return null;
        }
    }

    private void handleEvent(Map<String, Object> event) {
        String eventType = (String)event.get("type");
        String text = (String)event.get("text");
        String subtype = (String)event.get("subtype");
        String channelId = (String)event.get("channel");
        String ts = (String)event.get("ts");
        String threadTs = (String)event.get("thread_ts");

        log.debug("\uc774\ubca4\ud2b8 \ucc98\ub9ac: eventType={}", eventType);

        if (subtype != null) {
            return;
        }

        String effectiveThreadTs = threadTs != null ? threadTs : ts;

        if ("message".equals(eventType) && text != null) {
            if (slackAIService.isAIQuery(text)) {
                log.debug("AI \uc9c8\ubb38 \uac10\uc9c0");
                slackAIService.processAIQuery(text, channelId, effectiveThreadTs, null);
            }
        }

        if ("app_mention".equals(eventType) && text != null) {
            String normalizedText = slackAIService.normalizeAppMentionText(text);
            log.debug("\uc571 \uba58\uc158 \uac10\uc9c0");
            if (threadTs != null) {
                List<Map<String, Object>> aiReplies =
                    slackAIService.fetchAIThreadReplies(channelId, threadTs);
                slackAIService.processAIQuery(
                    normalizedText, channelId, effectiveThreadTs,
                    aiReplies.isEmpty() ? null : aiReplies);
            } else {
                slackAIService.processAIQuery(normalizedText, channelId, effectiveThreadTs, null);
            }
        }
    }
}
