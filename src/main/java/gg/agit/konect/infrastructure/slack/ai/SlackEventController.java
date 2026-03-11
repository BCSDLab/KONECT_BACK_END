package gg.agit.konect.infrastructure.slack.ai;

import java.util.List;
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
            log.warn("Slack \uc694\uccad \ubcf8\ubb38 \ud30c\uc2f1 \uc2e4\ud328");
            return ResponseEntity.badRequest().build();
        }

        String type = (String)payload.get("type");

        // URL \uac80\uc99d\uc740 \uc11c\uba85 \uac80\uc99d \uc5c6\uc774 \ucc98\ub9ac (\ucd5c\ucd08 \uc124\uc815 \uc2dc)
        if ("url_verification".equals(type)) {
            String challenge = (String)payload.get("challenge");
            log.info("Slack URL \uac80\uc99d \uc694\uccad \ucc98\ub9ac");
            return ResponseEntity.ok(Map.of("challenge", challenge));
        }

        // \uc11c\uba85 \uac80\uc99d - \uc6d0\ubcf8 \uc694\uccad \ubcf8\ubb38 \uc0ac\uc6a9
        if (!signatureVerifier.isValidRequest(timestamp, signature, rawBody)) {
            log.warn("Slack \uc11c\uba85 \uac80\uc99d \uc2e4\ud328");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.debug("Slack \uc774\ubca4\ud2b8 \uc218\uc2e0: type={}", type);

        // \uc774\ubca4\ud2b8 \ucf5c\ubc31 \ucc98\ub9ac
        if ("event_callback".equals(type)) {
            Map<String, Object> event = (Map<String, Object>)payload.get("event");
            if (event != null) {
                handleEvent(event);
            }
        }

        // Slack\uc740 3\ucd08 \ub0b4 \uc751\ub2f5\uc744 \uae30\ub300\ud558\ubbc0\ub85c \ube60\ub974\uac8c 200 \ubc18\ud658
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

        // bot \uba54\uc2dc\uc9c0\ub098 \ubcc0\uacbd \uc774\ubca4\ud2b8\ub294 \ubb34\uc2dc
        if (subtype != null) {
            return;
        }

        // \uc2a4\ub808\ub4dc \ub8e8\ud2b8 ts \uacb0\uc815: thread_ts\uac00 \uc788\uc73c\uba74 \uc2a4\ub808\ub4dc \ub0b4 \uba54\uc2dc\uc9c0, \uc5c6\uc73c\uba74 \uc0c8 \uc2a4\ub808\ub4dc \uc2dc\uc791
        String effectiveThreadTs = threadTs != null ? threadTs : ts;

        // \uba54\uc2dc\uc9c0 \uc774\ubca4\ud2b8 \ucc98\ub9ac
        if ("message".equals(eventType) && text != null) {
            if (slackAIService.isAIQuery(text)) {
                log.debug("AI \uc9c8\ubb38 \uac10\uc9c0");
                slackAIService.processAIQuery(text, channelId, effectiveThreadTs, null);
            } else if (threadTs != null) {
                // AI \uc2a4\ub808\ub4dc \ud655\uc778\uacfc replies fetch\ub97c \ud55c \ubc88\uc5d0 \uccb4\ud06c (\uc911\ubcf5 API \ud638\ucd9c \ubc29\uc9c0)
                List<Map<String, Object>> aiReplies =
                    slackAIService.fetchAIThreadReplies(channelId, threadTs);
                if (!aiReplies.isEmpty()) {
                    log.debug("AI \uc2a4\ub808\ub4dc \ub0b4 \ud6c4\uc18d \uc9c8\ubb38 \uac10\uc9c0");
                    slackAIService.processAIQuery(text, channelId, effectiveThreadTs, aiReplies);
                }
            }
        }

        // \uc571 \uba58\uc158 \uc774\ubca4\ud2b8 \ucc98\ub9ac
        if ("app_mention".equals(eventType) && text != null) {
            String normalizedText = slackAIService.normalizeAppMentionText(text);
            log.debug("\uc571 \uba58\uc158 \uac10\uc9c0");
            slackAIService.processAIQuery(normalizedText, channelId, effectiveThreadTs, null);
        }
    }
}
