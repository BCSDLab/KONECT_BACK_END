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

    @Async
    public void processAIQuery(String text) {
        try {
            String query = extractQuery(text);
            log.info("AI 질문 처리 시작: {}", query);

            // 1. Gemini에게 의도 분석 요청
            String queryType = geminiClient.analyzeIntent(query);
            log.info("분석된 쿼리 타입: {}", queryType);

            // 2. 의도에 따른 데이터 조회
            String data = queryExecutor.execute(queryType);
            log.info("조회된 데이터: {}", data);

            // 3. Gemini에게 자연어 응답 생성 요청
            String response = geminiClient.generateResponse(query, data);
            log.info("생성된 응답: {}", response);

            // 4. Slack에 응답 전송
            String slackMessage = formatSlackResponse(response);
            slackClient.sendMessage(slackMessage, slackProperties.webhooks().event());

        } catch (Exception e) {
            log.error("AI 질문 처리 중 오류 발생", e);
            String errorMessage = ":warning: 죄송합니다. 요청을 처리하는 중 오류가 발생했습니다.";
            slackClient.sendMessage(errorMessage, slackProperties.webhooks().event());
        }
    }

    private String formatSlackResponse(String response) {
        return String.format("""
            :robot_face: *AI 응답*
            %s
            """, response);
    }
}
