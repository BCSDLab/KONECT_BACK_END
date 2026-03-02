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
    private static final String UNSUPPORTED = "UNSUPPORTED";

    private final GeminiClient geminiClient;
    private final DynamicQueryExecutor queryExecutor;
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

    @Async
    public void processAIQuery(String text) {
        try {
            String userQuery = extractQuery(text);
            log.info("AI 질문 처리 시작: {}", userQuery);

            // 1. Gemini에게 JPQL 쿼리 생성 요청
            String jpqlQuery = geminiClient.generateQuery(userQuery);
            log.info("생성된 JPQL: {}", jpqlQuery);

            String response;

            // 2. 지원하지 않는 질문인 경우
            if (UNSUPPORTED.equalsIgnoreCase(jpqlQuery.trim())) {
                response = generateUnsupportedResponse(userQuery);
            } else {
                // 3. 쿼리 실행
                String data = queryExecutor.executeQuery(jpqlQuery);
                log.info("조회된 데이터: {}", data);

                // 4. Gemini에게 자연어 응답 생성 요청
                response = geminiClient.generateResponse(userQuery, data);
            }

            log.info("생성된 응답: {}", response);

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
            "이 질문은 KONECT 서비스 데이터와 관련이 없어서 답변하기 어렵습니다. "
                + "사용자 수, 동아리 정보, 일정, 공부 시간 등 서비스 관련 질문을 해주세요."
        );
    }

    private String formatSlackResponse(String response) {
        return String.format("""
            :robot_face: *AI 응답*
            %s
            """, response);
    }
}
