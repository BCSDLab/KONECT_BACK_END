package gg.agit.konect.infrastructure.slack.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import gg.agit.konect.infrastructure.claude.client.ClaudeClient;
import gg.agit.konect.infrastructure.slack.client.SlackClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackAIService {

    private static final Pattern AI_PREFIX_PATTERN = Pattern.compile("^[Aa][Ii]\\)\\s*(.+)$");
    private static final Pattern MENTION_PATTERN = Pattern.compile("^<@[^>]+>\\s*");
    private static final String AI_RESPONSE_PREFIX = ":robot_face: *AI 응답*\n";

    private final ClaudeClient claudeClient;
    private final SlackClient slackClient;

    public boolean isAIQuery(String text) {
        if (text == null) {
            return false;
        }
        return AI_PREFIX_PATTERN.matcher(text.trim()).matches();
    }

    public String extractQuery(String text) {
        Matcher matcher = AI_PREFIX_PATTERN.matcher(text.trim());
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return text;
    }

    public String normalizeAppMentionText(String text) {
        if (text == null) {
            return null;
        }
        return MENTION_PATTERN.matcher(text).replaceFirst("").trim();
    }

    @Async
    public void processAIQuery(String text, String channelId, String threadTs) {
        try {
            String userQuery = extractQuery(text);

            if (userQuery == null || userQuery.isBlank()) {
                log.debug("빈 질문으로 처리 중단");
                slackClient.postThreadReply(channelId, threadTs,
                    formatSlackResponse("질문 내용이 비어있습니다. 예: `AI) 가입자 수 알려줘` 또는 `@봇이름 동아리 수는?`"));
                return;
            }

            log.debug("AI 질문 처리 시작: {}", userQuery);

            List<Map<String, Object>> messages = buildConversationHistory(channelId, threadTs);

            if (messages.isEmpty()) {
                messages = new ArrayList<>();
                messages.add(Map.of("role", "user", "content", userQuery));
            }

            String response = claudeClient.chat(messages);

            log.debug("AI 응답 생성 완료");

            slackClient.postThreadReply(channelId, threadTs, formatSlackResponse(response));

        } catch (Exception e) {
            log.error("AI 질문 처리 중 오류 발생", e);
            slackClient.postThreadReply(channelId, threadTs,
                ":warning: 죄송합니다. 요청을 처리하는 중 오류가 발생했습니다.");
        }
    }

    private List<Map<String, Object>> buildConversationHistory(String channelId, String threadTs) {
        List<Map<String, Object>> replies = slackClient.getThreadReplies(channelId, threadTs);

        if (replies.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        for (Map<String, Object> reply : replies) {
            String replyText = (String)reply.get("text");

            if (replyText == null) {
                continue;
            }

            if (reply.get("bot_id") != null) {
                String content = replyText.replace(AI_RESPONSE_PREFIX, "");
                messages.add(Map.of("role", "assistant", "content", content));
            } else {
                String userText = isAIQuery(replyText) ? extractQuery(replyText) : replyText;
                messages.add(Map.of("role", "user", "content", userText));
            }
        }

        return messages;
    }

    private String formatSlackResponse(String response) {
        return String.format(":robot_face: *AI 응답*\n%s", response);
    }
}
