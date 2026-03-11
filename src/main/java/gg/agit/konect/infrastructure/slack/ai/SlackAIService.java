package gg.agit.konect.infrastructure.slack.ai;

import java.util.ArrayList;
import java.util.HashMap;
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
    private static final String EMPTY_QUERY_MESSAGE =
        "질문 내용이 비어있습니다. 예: `AI) 가입자 수 알려줘` 또는 `@봇이름 동아리 수는?`";
    private static final String ERROR_MESSAGE =
        ":warning: 죄송합니다. 요청을 처리하는 중 오류가 발생했습니다.";

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

    /**
     * AI 스레드이면 replies 반환, 아니면 빈 리스트 반환.
     * Controller에서 isAIThread 판단과 getThreadReplies를 한 번에 처리하도록 통합.
     */
    public List<Map<String, Object>> fetchAIThreadReplies(String channelId, String threadTs) {
        List<Map<String, Object>> replies = slackClient.getThreadReplies(channelId, threadTs);
        if (replies.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, Object> rootMessage = replies.get(0);
        String rootText = (String)rootMessage.get("text");
        if (rootText != null && isAIQuery(rootText)) {
            return replies;
        }
        if (replies.stream().anyMatch(r -> r.get("bot_id") != null)) {
            return replies;
        }
        return new ArrayList<>();
    }

    @Async
    public void processAIQuery(String text, String channelId, String threadTs,
            List<Map<String, Object>> cachedReplies) {
        try {
            String userQuery = extractQuery(text);

            if (userQuery == null || userQuery.isBlank()) {
                log.debug("빈 질문으로 처리 중단");
                slackClient.postThreadReply(channelId, threadTs,
                    formatSlackResponse(EMPTY_QUERY_MESSAGE));
                return;
            }

            log.debug("AI 질문 처리 시작: {}", userQuery);

            List<Map<String, Object>> replies =
                cachedReplies != null ? cachedReplies : new ArrayList<>();
            List<Map<String, Object>> messages = buildConversationHistory(replies);

            if (messages.isEmpty()) {
                messages = new ArrayList<>();
                messages.add(Map.of("role", "user", "content", userQuery));
            }

            String response = claudeClient.chat(messages);

            log.debug("AI 응답 생성 완료");

            slackClient.postThreadReply(channelId, threadTs, formatSlackResponse(response));

        } catch (Exception e) {
            log.error("AI 질문 처리 중 오류 발생", e);
            slackClient.postThreadReply(channelId, threadTs, ERROR_MESSAGE);
        }
    }

    private List<Map<String, Object>> buildConversationHistory(List<Map<String, Object>> replies) {
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
                String content = replyText.startsWith(AI_RESPONSE_PREFIX)
                    ? replyText.substring(AI_RESPONSE_PREFIX.length())
                    : replyText;
                messages.add(Map.of("role", "assistant", "content", content));
            } else {
                String normalizedText = normalizeAppMentionText(replyText);
                String userText = isAIQuery(normalizedText)
                    ? extractQuery(normalizedText)
                    : normalizedText;
                messages.add(Map.of("role", "user", "content", userText));
            }
        }

        return mergeConsecutiveRoles(messages);
    }

    /**
     * Claude API는 user/assistant이 교대로 와야 하므로
     * 연속된 동일 role 메시지를 하나로 병합.
     */
    private List<Map<String, Object>> mergeConsecutiveRoles(List<Map<String, Object>> messages) {
        List<Map<String, Object>> merged = new ArrayList<>();
        for (Map<String, Object> msg : messages) {
            if (!merged.isEmpty()
                    && merged.get(merged.size() - 1).get("role").equals(msg.get("role"))) {
                Map<String, Object> last = new HashMap<>(merged.get(merged.size() - 1));
                last.put("content", last.get("content") + "\n" + msg.get("content"));
                merged.set(merged.size() - 1, last);
            } else {
                merged.add(msg);
            }
        }
        return merged;
    }

    private String formatSlackResponse(String response) {
        return String.format(":robot_face: *AI 응답*\n%s", response);
    }
}
