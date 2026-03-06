package gg.agit.konect.infrastructure.gemini.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.infrastructure.gemini.config.GeminiProperties;
import gg.agit.konect.infrastructure.mcp.client.McpClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GeminiClient {

    private static final String API_URL_TEMPLATE =
        "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private static final String API_KEY_HEADER = "x-goog-api-key";

    private static final double GENERATION_TEMPERATURE = 0.3;
    private static final int MAX_OUTPUT_TOKENS = 1024;

    private static final String SYSTEM_PROMPT = """
        당신은 KONECT 서비스의 데이터 분석 AI입니다.
        사용자 질문에 답하기 위해 query 도구를 사용하여 MySQL 데이터베이스를 조회하세요.
        SELECT 문만 사용 가능합니다.
        반드시 한국어로만 응답하세요.

        주요 테이블 및 컬럼:

        1. users (사용자)
           - id, email, nickname, created_at, updated_at, deleted_at
           - deleted_at IS NULL 조건으로 활성 사용자 필터링
           - created_at으로 가입일 조회 (예: DATE(created_at) = CURDATE() - INTERVAL 1 DAY)

        2. club (동아리)
           - id, name, description, created_at, updated_at

        3. club_member (동아리 멤버)
           - id, club_id, user_id, role, created_at
           - role: PRESIDENT, VICE_PRESIDENT, MEMBER
           - club_id로 club 테이블과 JOIN 가능

        4. club_recruitment (모집 공고)
           - id, club_id, is_always_recruiting, start_at, end_at, created_at
           - 모집 중 조건: is_always_recruiting = true OR (start_at <= NOW() AND end_at >= NOW())
           - club_id로 club 테이블과 JOIN 가능

        자주 사용하는 쿼리 예시:
        - 동아리 멤버 수: SELECT COUNT(*) FROM club_member cm
          JOIN club c ON cm.club_id = c.id WHERE c.name = '동아리명'
        - 어제 가입 회원: SELECT COUNT(*) FROM users
          WHERE DATE(created_at) = CURDATE() - INTERVAL 1 DAY AND deleted_at IS NULL

        질문에 적절한 SQL을 작성하고 query 도구를 호출하세요.
        결과를 받으면 사용자에게 친절하고 자연스러운 한국어로 응답하세요.
        이모지를 적절히 사용하여 친근하게 답변하세요.
        응답은 간결하게 2-3문장으로 작성하세요.

        지원하지 않는 질문(데이터베이스와 무관한 질문)에는 정중히 거절하고,
        어떤 질문이 가능한지 안내하세요.
        """;

    private static final Map<String, Object> QUERY_TOOL = Map.of(
        "name", "query",
        "description", "MySQL 데이터베이스에 SELECT 쿼리를 실행합니다. 반드시 SELECT 문만 사용하세요.",
        "parameters", Map.of(
            "type", "object",
            "properties", Map.of(
                "sql", Map.of(
                    "type", "string",
                    "description", "실행할 SELECT SQL 쿼리"
                )
            ),
            "required", List.of("sql")
        )
    );

    private final RestClient restClient;
    private final GeminiProperties geminiProperties;
    private final McpClient mcpClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(RestClient.Builder restClientBuilder,
                        GeminiProperties geminiProperties,
                        McpClient mcpClient,
                        ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.geminiProperties = geminiProperties;
        this.mcpClient = mcpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Process user query with function calling support.
     *
     * @param userMessage User's question
     * @return AI response
     */
    public String chat(String userMessage) {
        try {
            // 1. First call - may result in tool call
            Map<String, Object> request = buildInitialRequest(userMessage);
            String response = callGeminiApi(request);

            JsonNode responseNode = objectMapper.readTree(response);

            // 2. Check for function call
            JsonNode functionCall = extractFunctionCall(responseNode);
            if (functionCall != null) {
                String toolName = functionCall.path("name").asText();
                JsonNode args = functionCall.path("args");

                if ("query".equals(toolName) && args.has("sql")) {
                    String sql = args.get("sql").asText();
                    log.debug("Executing SQL via MCP");

                    // 3. Execute query via MCP
                    String queryResult;
                    try {
                        queryResult = mcpClient.executeQuery(sql);
                    } catch (McpClient.McpQueryException e) {
                        queryResult = "쿼리 실행 오류: " + e.getMessage();
                    }

                    // 4. Send result back to Gemini
                    return callWithToolResult(userMessage, toolName, sql, queryResult);
                }
            }

            // No function call - return direct response
            return extractTextResponse(responseNode);

        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            return "죄송합니다. 요청을 처리하는 중 오류가 발생했습니다.";
        }
    }

    private Map<String, Object> buildInitialRequest(String userMessage) {
        Map<String, Object> request = new HashMap<>();

        // System instruction
        request.put("systemInstruction", Map.of(
            "parts", List.of(Map.of("text", SYSTEM_PROMPT))
        ));

        // User message
        request.put("contents", List.of(
            Map.of("role", "user", "parts", List.of(Map.of("text", userMessage)))
        ));

        // Tools (function declarations)
        request.put("tools", List.of(
            Map.of("functionDeclarations", List.of(QUERY_TOOL))
        ));

        // Generation config
        request.put("generationConfig", Map.of(
            "temperature", GENERATION_TEMPERATURE,
            "maxOutputTokens", MAX_OUTPUT_TOKENS
        ));

        return request;
    }

    private String callWithToolResult(String userMessage, String toolName, String sql, String result) {
        try {
            Map<String, Object> request = new HashMap<>();

            // System instruction
            request.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", SYSTEM_PROMPT))
            ));

            // Conversation history with tool call and result
            List<Map<String, Object>> contents = new ArrayList<>();

            // User message
            contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
            ));

            // Model's function call
            contents.add(Map.of(
                "role", "model",
                "parts", List.of(Map.of(
                    "functionCall", Map.of(
                        "name", toolName,
                        "args", Map.of("sql", sql)
                    )
                ))
            ));

            // Function result
            contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of(
                    "functionResponse", Map.of(
                        "name", toolName,
                        "response", Map.of("result", result)
                    )
                ))
            ));

            request.put("contents", contents);

            // Tools still available for potential multi-turn
            request.put("tools", List.of(
                Map.of("functionDeclarations", List.of(QUERY_TOOL))
            ));

            request.put("generationConfig", Map.of(
                "temperature", GENERATION_TEMPERATURE,
                "maxOutputTokens", MAX_OUTPUT_TOKENS
            ));

            String response = callGeminiApi(request);
            JsonNode responseNode = objectMapper.readTree(response);

            return extractTextResponse(responseNode);

        } catch (Exception e) {
            log.error("Failed to process tool result", e);
            return "쿼리 결과를 처리하는 중 오류가 발생했습니다.";
        }
    }

    private String callGeminiApi(Map<String, Object> request) {
        String url = String.format(API_URL_TEMPLATE, geminiProperties.model());

        try {
            String response = restClient.post()
                .uri(url)
                .header(API_KEY_HEADER, geminiProperties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

            return response;

        } catch (RestClientException e) {
            log.error("Gemini API call failed");
            throw new GeminiException("Gemini API call failed", e);
        }
    }

    private JsonNode extractFunctionCall(JsonNode response) {
        JsonNode candidates = response.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode content = candidates.get(0).path("content");
            JsonNode parts = content.path("parts");
            if (parts.isArray()) {
                // Iterate through all parts to find functionCall
                for (JsonNode part : parts) {
                    if (part.has("functionCall")) {
                        return part.get("functionCall");
                    }
                }
            }
        }
        return null;
    }

    private String extractTextResponse(JsonNode response) {
        JsonNode candidates = response.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode content = candidates.get(0).path("content");
            JsonNode parts = content.path("parts");
            if (parts.isArray()) {
                // Combine all text parts
                StringBuilder textBuilder = new StringBuilder();
                for (JsonNode part : parts) {
                    if (part.has("text")) {
                        textBuilder.append(part.get("text").asText());
                    }
                }
                if (!textBuilder.isEmpty()) {
                    return textBuilder.toString();
                }
            }
        }
        return "응답을 생성할 수 없습니다.";
    }

    public static class GeminiException extends RuntimeException {
        public GeminiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
