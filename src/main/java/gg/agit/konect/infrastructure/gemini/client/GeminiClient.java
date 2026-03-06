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
    private static final int MAX_TOOL_ITERATIONS = 5;

    private static final String SYSTEM_PROMPT = """
        당신은 KONECT 서비스의 데이터 분석 AI입니다.
        사용자 질문에 답하기 위해 데이터베이스를 조회하세요.
        SELECT 문만 사용 가능합니다.
        반드시 한국어로만 응답하세요.

        사용 가능한 도구:
        1. list_tables: 데이터베이스의 모든 테이블 목록 조회
        2. describe_table: 특정 테이블의 컬럼 구조 조회 (테이블명 필요)
        3. query: SQL SELECT 쿼리 실행

        작업 순서:
        1. 질문에 필요한 테이블을 모르면 list_tables로 테이블 목록 확인
        2. 테이블 구조를 모르면 describe_table로 컬럼 정보 확인
        3. 충분한 정보가 있으면 query로 SQL 실행

        알려진 주요 테이블:
        - users: 사용자 (deleted_at IS NULL = 활성 사용자)
        - club: 동아리
        - club_member: 동아리 멤버 (role: PRESIDENT, VICE_PRESIDENT, MEMBER)
        - club_recruitment: 모집 공고
        - club_apply: 동아리 지원

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

    private static final Map<String, Object> LIST_TABLES_TOOL = Map.of(
        "name", "list_tables",
        "description", "데이터베이스의 모든 테이블 목록을 조회합니다.",
        "parameters", Map.of(
            "type", "object",
            "properties", Map.of()
        )
    );

    private static final Map<String, Object> DESCRIBE_TABLE_TOOL = Map.of(
        "name", "describe_table",
        "description", "특정 테이블의 컬럼 구조를 조회합니다.",
        "parameters", Map.of(
            "type", "object",
            "properties", Map.of(
                "table_name", Map.of(
                    "type", "string",
                    "description", "조회할 테이블 이름"
                )
            ),
            "required", List.of("table_name")
        )
    );

    private static final List<Map<String, Object>> ALL_TOOLS = List.of(
        QUERY_TOOL, LIST_TABLES_TOOL, DESCRIBE_TABLE_TOOL
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
     * Supports multi-turn tool calls for schema discovery and query execution.
     *
     * @param userMessage User's question
     * @return AI response
     */
    public String chat(String userMessage) {
        try {
            List<Map<String, Object>> conversationHistory = new ArrayList<>();

            // Add user message
            conversationHistory.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
            ));

            // Multi-turn loop for tool calls
            for (int i = 0; i < MAX_TOOL_ITERATIONS; i++) {
                Map<String, Object> request = buildRequestWithHistory(conversationHistory);
                String response = callGeminiApi(request);
                JsonNode responseNode = objectMapper.readTree(response);

                JsonNode functionCall = extractFunctionCall(responseNode);
                if (functionCall == null) {
                    // No more tool calls - return text response
                    return extractTextResponse(responseNode);
                }

                String toolName = functionCall.path("name").asText();
                JsonNode args = functionCall.path("args");
                String toolResult = executeToolCall(toolName, args);

                // Add model's function call to history
                conversationHistory.add(Map.of(
                    "role", "model",
                    "parts", List.of(Map.of(
                        "functionCall", Map.of(
                            "name", toolName,
                            "args", objectMapper.convertValue(args, Map.class)
                        )
                    ))
                ));

                // Add function result to history
                conversationHistory.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of(
                        "functionResponse", Map.of(
                            "name", toolName,
                            "response", Map.of("result", toolResult)
                        )
                    ))
                ));
            }

            return "죄송합니다. 요청 처리 중 최대 반복 횟수에 도달했습니다.";

        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            return "죄송합니다. 요청을 처리하는 중 오류가 발생했습니다.";
        }
    }

    private String executeToolCall(String toolName, JsonNode args) {
        try {
            return switch (toolName) {
                case "query" -> {
                    String sql = args.path("sql").asText();
                    log.debug("Executing SQL via MCP: {}", sql);
                    yield mcpClient.executeQuery(sql);
                }
                case "list_tables" -> {
                    log.debug("Listing tables via MCP");
                    List<String> tables = mcpClient.listTables();
                    yield String.join(", ", tables);
                }
                case "describe_table" -> {
                    String tableName = args.path("table_name").asText();
                    log.debug("Describing table via MCP: {}", tableName);
                    yield mcpClient.describeTable(tableName);
                }
                default -> "알 수 없는 도구: " + toolName;
            };
        } catch (McpClient.McpQueryException e) {
            return "도구 실행 오류: " + e.getMessage();
        }
    }

    private Map<String, Object> buildRequestWithHistory(List<Map<String, Object>> conversationHistory) {
        Map<String, Object> request = new HashMap<>();

        // System instruction
        request.put("systemInstruction", Map.of(
            "parts", List.of(Map.of("text", SYSTEM_PROMPT))
        ));

        // Conversation history
        request.put("contents", conversationHistory);

        // Tools (all function declarations)
        request.put("tools", List.of(
            Map.of("functionDeclarations", ALL_TOOLS)
        ));

        // Generation config
        request.put("generationConfig", Map.of(
            "temperature", GENERATION_TEMPERATURE,
            "maxOutputTokens", MAX_OUTPUT_TOKENS
        ));

        return request;
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
