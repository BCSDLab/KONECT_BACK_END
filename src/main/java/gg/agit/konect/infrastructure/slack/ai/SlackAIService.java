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
    private static final Pattern MARKDOWN_BOLD_PATTERN =
        Pattern.compile("\\*\\*(.+?)\\*\\*", Pattern.DOTALL);
    private static final String AI_RESPONSE_PREFIX = ":robot_face: *AI \uc751\ub2f5*\n";
    private static final int MAX_HISTORY_MESSAGES = 10;
    private static final String EMPTY_QUERY_MESSAGE =
        "\uc9c8\ubb38 \ub0b4\uc6a9\uc774 \ube44\uc5b4\uc788\uc2b5\ub2c8\ub2e4. \uc608: `AI) \uac00\uc785\uc790 \uc218 \uc54c\ub824\uc918` \ub610\ub294 `@\ubd07\uc774\ub984 \ub3d9\uc544\ub9ac \uc218\ub294?`";
    private static final String ERROR_MESSAGE =
        ":warning: \uc8c4\uc1a1\ud569\ub2c8\ub2e4. \uc694\uccad\uc744 \ucc98\ub9ac\ud558\ub294 \uc911 \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4.";

    private final ClaudeClient claudeClient;
    private final SlackClient slackClient;

    public boolean isAIQuery(String text) {
        if (text == null) {
            return false;
        }
        return AI_PREFIX_PATTERN.matcher(text.trim()).matches();
    }

    public boolean isAppMention(String text) {
        if (text == null) {
            return false;
        }
        return MENTION_PATTERN.matcher(text.trim()).find();
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
                log.debug("\ube48 \uc9c8\ubb38\uc73c\ub85c \ucc98\ub9ac \uc911\ub2e8");
                slackClient.postThreadReply(channelId, threadTs,
                    formatSlackResponse(EMPTY_QUERY_MESSAGE));
                return;
            }

            log.debug("AI \uc9c8\ubb38 \ucc98\ub9ac \uc2dc\uc791: {}", userQuery);

            List<Map<String, Object>> replies =
                cachedReplies != null ? cachedReplies : new ArrayList<>();
            List<Map<String, Object>> messages = buildConversationHistory(replies);

            if (messages.isEmpty()) {
                messages = new ArrayList<>();
                messages.add(Map.of("role", "user", "content", userQuery));
            }

            String response = claudeClient.chat(messages);

            log.debug("AI \uc751\ub2f5 \uc0dd\uc131 \uc644\ub8cc");

            slackClient.postThreadReply(channelId, threadTs, formatSlackResponse(response));

        } catch (Exception e) {
            log.error("AI \uc9c8\ubb38 \ucc98\ub9ac \uc911 \uc624\ub958 \ubc1c\uc0dd", e);
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

        List<Map<String, Object>> merged = mergeConsecutiveRoles(messages);

        if (!merged.isEmpty() && "assistant".equals(merged.get(0).get("role"))) {
            merged = new ArrayList<>(merged.subList(1, merged.size()));
        }

        if (merged.size() > MAX_HISTORY_MESSAGES) {
            merged = new ArrayList<>(
                merged.subList(merged.size() - MAX_HISTORY_MESSAGES, merged.size())
            );
        }
        return merged;
    }

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

    private String convertMarkdownToSlack(String text) {
        if (text == null) {
            return null;
        }
        return MARKDOWN_BOLD_PATTERN.matcher(text).replaceAll("*$1*");
    }

    private String formatSlackResponse(String response) {
        return AI_RESPONSE_PREFIX + convertMarkdownToSlack(response);
    }
}
