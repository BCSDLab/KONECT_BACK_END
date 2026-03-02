package gg.agit.konect.infrastructure.slack.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import gg.agit.konect.infrastructure.gemini.client.GeminiClient;
import gg.agit.konect.infrastructure.slack.client.SlackClient;
import gg.agit.konect.infrastructure.slack.config.SlackProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackAIService {

    private static final Pattern AI_PREFIX_PATTERN = Pattern.compile("^[Aa][Ii]\\)\\s*(.+)$");
    private static final Pattern MENTION_PATTERN = Pattern.compile("^<@[^>]+>\\s*");
    private static final String UNKNOWN = "UNKNOWN";

    private final GeminiClient geminiClient;
    private final StatisticsQueryExecutor queryExecutor;
    private final SlackClient slackClient;
    private final SlackProperties slackProperties;

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
    public void processAIQuery(String text) {
        try {
            String userQuery = extractQuery(text);

            // 빈 질문은 처리하지 않음
            if (userQuery == null || userQuery.isBlank()) {
                log.debug("빈 질문으로 처리 중단");
                String guidanceMessage = formatSlackResponse(
                    "질문 내용이 비어있습니다. 예: `AI) 가입자 수 알려줘` 또는 `@봇이름 동아리 수는?`"
                );
                slackClient.sendMessage(guidanceMessage, slackProperties.webhooks().event());
                return;
            }

            log.debug("AI 질문 처리 시작");

            // 1. Gemini에게 의도 분석 요청
            String queryType = geminiClient.analyzeIntent(userQuery);
            log.debug("분석된 쿼리 타입: {}", queryType);

            String response;

            // 2. 지원하지 않는 질문인 경우
            if (UNKNOWN.equals(queryType)) {
                response = generateUnsupportedResponse(userQuery);
            } else {
                // 3. 안전한 통계 쿼리 실행
                String data = queryExecutor.execute(queryType);

                if (data == null) {
                    response = generateUnsupportedResponse(userQuery);
                } else {
                    // 4. Gemini에게 자연어 응답 생성 요청
                    response = geminiClient.generateResponse(userQuery, data);
                }
            }

            log.debug("AI 응답 생성 완료");

            // 5. Slack에 응답 전송
            String slackMessage = formatSlackResponse(response);
            slackClient.sendMessage(slackMessage, slackProperties.webhooks().event());

        } catch (Exception e) {
            log.error("AI 질문 처리 중 오류 발생", e);
            String errorMessage = ":warning: 죄송합니다. 요청을 처리하는 중 오류가 발생했습니다.";
            slackClient.sendMessage(errorMessage, slackProperties.webhooks().event());
        }
    }

    private String generateUnsupportedResponse(String userQuery) {
        return geminiClient.generateResponse(
            userQuery,
            "이 질문은 현재 지원하지 않는 유형입니다. "
                + "사용자 수, 동아리 수, 모집 현황, 동아리원 수 등의 통계 질문을 해주세요."
        );
    }

    private String formatSlackResponse(String response) {
        return String.format(":robot_face: *AI 응답*\n%s", response);
    }
}
