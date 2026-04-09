package gg.agit.konect.infrastructure.slack.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import gg.agit.konect.infrastructure.slack.config.SlackSignatureVerifier;
import gg.agit.konect.support.ControllerTestSupport;

@WebMvcTest(SlackEventController.class)
@WithMockUser
class SlackEventControllerTest extends ControllerTestSupport {

    @MockitoBean
    private SlackAIService slackAIService;

    @MockitoBean
    private SlackSignatureVerifier slackSignatureVerifier;

    @Nested
    @DisplayName("POST /slack/events - Slack 이벤트 처리")
    class HandleSlackEvent {

        @Test
        @DisplayName("url_verification 요청은 서명 검증 없이 challenge를 반환한다")
        void handleUrlVerification() throws Exception {
            String rawBody = """
                {
                  "type": "url_verification",
                  "challenge": "challenge-token"
                }
                """;

            mockMvc.perform(post("/slack/events")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challenge").value("challenge-token"));

            verify(slackSignatureVerifier, never()).isValidRequest(any(), any(), any());
        }

        @Test
        @DisplayName("같은 event_id의 중복 이벤트는 한 번만 처리한다")
        void ignoresDuplicateEventId() throws Exception {
            given(slackSignatureVerifier.isValidRequest(any(), any(), any())).willReturn(true);
            given(slackAIService.isAIQuery("AI) 중복 처리 확인")).willReturn(true);

            String rawBody = """
                {
                  "type": "event_callback",
                  "event_id": "event-1",
                  "event": {
                    "type": "message",
                    "text": "AI) 중복 처리 확인",
                    "channel": "C123",
                    "ts": "1710000000.000100"
                  }
                }
                """;

            mockMvc.perform(post("/slack/events")
                    .with(csrf())
                    .header("X-Slack-Request-Timestamp", "1710000000")
                    .header("X-Slack-Signature", "v0=test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawBody))
                .andExpect(status().isOk());

            mockMvc.perform(post("/slack/events")
                    .with(csrf())
                    .header("X-Slack-Request-Timestamp", "1710000000")
                    .header("X-Slack-Signature", "v0=test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawBody))
                .andExpect(status().isOk());

            verify(slackAIService, times(1))
                .processAIQuery("AI) 중복 처리 확인", "C123", "1710000000.000100", null);
        }

        @Test
        @DisplayName("스레드의 app_mention은 기존 AI 스레드 답글을 함께 전달한다")
        void appMentionWithThreadRepliesPassesContext() throws Exception {
            given(slackSignatureVerifier.isValidRequest(any(), any(), any())).willReturn(true);
            given(slackAIService.normalizeAppMentionText("<@U123> 이번 주 현황 알려줘"))
                .willReturn("이번 주 현황 알려줘");

            List<Map<String, Object>> aiReplies = List.of(
                Map.of("text", "<@U123> 지난주 요약", "ts", "1710000000.000100"),
                Map.of("bot_id", "B123", "text", ":robot_face: *AI 응답*\n지난주 답변", "ts", "1710000000.000200")
            );
            given(slackAIService.fetchAIThreadReplies("C123", "1710000000.000100")).willReturn(aiReplies);

            String rawBody = """
                {
                  "type": "event_callback",
                  "event_id": "event-thread-1",
                  "event": {
                    "type": "app_mention",
                    "text": "<@U123> 이번 주 현황 알려줘",
                    "channel": "C123",
                    "ts": "1710000000.000300",
                    "thread_ts": "1710000000.000100"
                  }
                }
                """;

            mockMvc.perform(post("/slack/events")
                    .with(csrf())
                    .header("X-Slack-Request-Timestamp", "1710000001")
                    .header("X-Slack-Signature", "v0=test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawBody))
                .andExpect(status().isOk());

            verify(slackAIService).processAIQuery(
                "이번 주 현황 알려줘",
                "C123",
                "1710000000.000100",
                aiReplies
            );
        }

        @Test
        @DisplayName("subtype이 있는 message 이벤트는 무시한다")
        void ignoresSubtypeMessage() throws Exception {
            given(slackSignatureVerifier.isValidRequest(any(), any(), any())).willReturn(true);

            String rawBody = """
                {
                  "type": "event_callback",
                  "event_id": "event-subtype-1",
                  "event": {
                    "type": "message",
                    "subtype": "bot_message",
                    "text": "AI) 응답하지 말아야 함",
                    "channel": "C123",
                    "ts": "1710000000.000400"
                  }
                }
                """;

            mockMvc.perform(post("/slack/events")
                    .with(csrf())
                    .header("X-Slack-Request-Timestamp", "1710000002")
                    .header("X-Slack-Signature", "v0=test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawBody))
                .andExpect(status().isOk());

            verify(slackAIService, never()).isAIQuery(any());
            verify(slackAIService, never()).processAIQuery(any(), any(), any(), any());
        }
    }
}
