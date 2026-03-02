package gg.agit.konect.infrastructure.slack.ai;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/slack/events")
@RequiredArgsConstructor
public class SlackEventController {

    private final SlackAIService slackAIService;

    @PostMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<Object> handleSlackEvent(@RequestBody Map<String, Object> payload) {
        String type = (String)payload.get("type");
        log.info("Slack 이벤트 수신: type={}", type);

        // Slack URL 검증 (최초 설정 시 Slack에서 호출)
        if ("url_verification".equals(type)) {
            String challenge = (String)payload.get("challenge");
            log.info("Slack URL 검증 요청: challenge={}", challenge);
            return ResponseEntity.ok(Map.of("challenge", challenge));
        }

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

    private void handleEvent(Map<String, Object> event) {
        String eventType = (String)event.get("type");
        String text = (String)event.get("text");
        String subtype = (String)event.get("subtype");

        log.info("이벤트 처리: eventType={}, text={}", eventType, text);

        // bot 메시지나 변경 이벤트는 무시
        if (subtype != null) {
            log.debug("subtype이 있는 이벤트 무시: subtype={}", subtype);
            return;
        }

        // 메시지 이벤트 처리
        if ("message".equals(eventType) && text != null) {
            if (slackAIService.isAIQuery(text)) {
                log.info("AI 질문 감지: {}", text);
                slackAIService.processAIQuery(text);
            }
        }

        // 앱 멘션 이벤트 처리
        if ("app_mention".equals(eventType) && text != null) {
            log.info("앱 멘션 감지: {}", text);
            slackAIService.processAIQuery(text);
        }
    }
}
