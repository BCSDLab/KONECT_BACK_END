package gg.agit.konect.infrastructure.claude.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.infrastructure.claude.config.ClaudeProperties;
import gg.agit.konect.infrastructure.mcp.client.McpClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ClaudeClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private static final int MAX_TOKENS = 1024;
    private static final int MAX_TOOL_ITERATIONS = 5;

    private static final String SYSTEM_PROMPT = """
        당신은 KONECT 서비스의 데이터 분석 AI 에이전트입니다.

        ## 역할
        사용자의 질문을 분석하고, 데이터베이스에서 필요한 데이터를 조회하여 답변합니다.

        ## 사용 가능한 도구
        1. list_tables: 데이터베이스의 모든 테이블 목록 조회
        2. describe_table: 특정 테이블의 컬럼 구조 조회
        3. query: SQL SELECT 쿼리 실행 (읽기 전용)

        ## 작업 방식
        1. 질문과 관련된 테이블이 확실하지 않으면 반드시 list_tables로 먼저 확인
        2. 테이블 구조가 필요하면 describe_table로 컬럼 정보 확인
        3. 적절한 SQL 쿼리를 작성하여 데이터 조회
        4. 결과를 바탕으로 친절하고 자연스럽게 답변

        ## 주요 테이블 힌트 (예시, 전체 목록은 list_tables로 확인)
        - users: 사용자 정보 (deleted_at IS NULL = 활성 사용자)
        - club: 동아리 정보
        - club_member: 동아리 멤버 (role: PRESIDENT, VICE_PRESIDENT, MANAGER, MEMBER)
        - club_recruitment: 모집 공고
        - club_apply: 동아리 지원
        - university_schedule: 학사 일정
        - council_notice: 학생회 공지사항
        - study_time_*: 공부 시간 관련 테이블

        ## 응답 규칙
        - 반드시 한국어로 응답
        - 이모지를 적절히 사용하여 친근하게
        - 간결하게 2-3문장으로 답변
        - 모르는 테이블이 있으면 먼저 탐색 후 답변
        - 예측/미래 추론 질문은 현재까지의 데이터만 제공하고 예측은 어렵다고 안내
        - 데이터베이스에 정말 없는 정보만 정중히 거절
        """;

    private static final Map<String, Object> QUERY_TOOL = Map.of(
        "name", "query",
        "description", "MySQL 데이터베이스에 SELECT 쿼리를 실행합니다. 읽기 전용입니다.",
        "input_schema", Map.of(
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
        "input_schema", Map.of(
            "type", "object",
            "properties", Map.of()
        )
    );

    private static final Map<String, Object> DESCRIBE_TABLE_TOOL = Map.of(
        "name", "describe_table",
        "description", "특정 테이블의 컬럼 구조를 조회합니다.",
        "input_schema", Map.of(
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
    private final ClaudeProperties claudeProperties;
    private final McpClient mcpClient;
    private final ObjectMapper objectMapper;

    public ClaudeClient(RestClient.Builder restClientBuilder,
                        ClaudeProperties claudeProperties,
                        McpClient mcpClient,
                        ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.claudeProperties = claudeProperties;
        this.mcpClient = mcpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Process user query with tool use support.
     * Supports multi-turn tool calls for schema discovery and query execution.
     *
     * @param userMessage User's question
     * @return AI response
     */
    public String chat(String userMessage) {
        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", userMessage));

            for (int i = 0; i < MAX_TOOL_ITERATIONS; i++) {
                Map<String, Object> request = buildRequest(messages);
                String response = callClaudeApi(request);

                if (response == null) {
                    log.error("Claude API returned null response");
                    throw new ClaudeException("Claude API returned null response", null);
                }

                JsonNode responseNode = objectMapper.readTree(response);
                String stopReason = responseNode.path("stop_reason").asText();
                JsonNode content = responseNode.path("content");

                if ("end_turn".equals(stopReason)) {
                    return extractTextResponse(content);
                }

                if ("tool_use".equals(stopReason)) {
                    List<Map<String, Object>> toolResults = processToolCalls(content);

                    if (toolResults.isEmpty()) {
                        log.warn("stop_reason is tool_use but no tool_use blocks found");
                        return extractTextResponse(content);
                    }

                    messages.add(Map.of(
                        "role", "assistant",
                        "content", objectMapper.convertValue(content, List.class)
                    ));
                    messages.add(Map.of(
                        "role", "user",
                        "content", toolResults
                    ));
                } else {
                    log.warn("Unexpected stop_reason: {}", stopReason);
                    return extractTextResponse(content);
                }
            }

            log.warn("Maximum tool iterations reached");
            throw new ClaudeException("Maximum tool iterations reached", null);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Claude API response", e);
            throw new ClaudeException("Failed to parse Claude API response", e);
        }
    }

    public String chat(String userMessage) {
        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", userMessage));

            for (int i = 0; i < MAX_TOOL_ITERATIONS; i++) {
                Map<String, Object> request = buildRequest(messages);
                String response = callClaudeApi(request);

                if (response == null) {
                    log.error("Claude API returned null response");
                    throw new ClaudeException("Claude API returned null response", null);
                }

                JsonNode responseNode = objectMapper.readTree(response);

                String stopReason = responseNode.path("stop_reason").asText();
                JsonNode content = responseNode.path("content");

                if ("end_turn".equals(stopReason)) {
                    return extractTextResponse(content);
                }

                if ("tool_use".equals(stopReason)) {
                    // Process tool calls and add results
                    List<Map<String, Object>> toolResults = processToolCalls(content);

                    if (toolResults.isEmpty()) {
                        log.warn("stop_reason is tool_use but no tool_use blocks found");
                        return extractTextResponse(content);
                    }

                    // Add assistant's response to messages
                    messages.add(Map.of(
                        "role", "assistant",
                        "content", objectMapper.convertValue(content, List.class)
                    ));

                    messages.add(Map.of(
                        "role", "user",
                        "content", toolResults
                    ));
                } else {
                    // Unexpected stop reason
                    log.warn("Unexpected stop_reason: {}", stopReason);
                    return extractTextResponse(content);
                }
            }

            log.warn("Maximum tool iterations reached");
            throw new ClaudeException("Maximum tool iterations reached", null);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Claude API response", e);
            throw new ClaudeException("Failed to parse Claude API response", e);
        }
    }

    private List<Map<String, Object>> processToolCalls(JsonNode content) {
        List<Map<String, Object>> toolResults = new ArrayList<>();

        for (JsonNode block : content) {
            if ("tool_use".equals(block.path("type").asText())) {
                String toolId = block.path("id").asText();
                String toolName = block.path("name").asText();
                JsonNode input = block.path("input");

                log.debug("Tool call: {}", toolName);
                String result = executeToolCall(toolName, input);
                log.debug("Tool execution completed: {}", toolName);

                toolResults.add(Map.of(
                    "type", "tool_result",
                    "tool_use_id", toolId,
                    "content", result
                ));
            }
        }

        return toolResults;
    }

    private String executeToolCall(String toolName, JsonNode input) {
        try {
            return switch (toolName) {
                case "query" -> {
                    String sql = input.path("sql").asText();
                    log.debug("Executing SQL query via MCP");
                    yield mcpClient.executeQuery(sql);
                }
                case "list_tables" -> {
                    log.debug("Listing tables via MCP");
                    List<String> tables = mcpClient.listTables();
                    yield String.join(", ", tables);
                }
                case "describe_table" -> {
                    String tableName = input.path("table_name").asText();
                    log.debug("Describing table via MCP: {}", tableName);
                    yield mcpClient.describeTable(tableName);
                }
                default -> "알 수 없는 도구: " + toolName;
            };
        } catch (McpClient.McpQueryException e) {
            log.error("Tool execution failed: {}", e.getMessage());
            return "도구 실행 오류: " + e.getMessage();
        }
    }

    private Map<String, Object> buildRequest(List<Map<String, Object>> messages) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", claudeProperties.model());
        request.put("max_tokens", MAX_TOKENS);
        request.put("system", SYSTEM_PROMPT);
        request.put("messages", messages);
        request.put("tools", ALL_TOOLS);
        return request;
    }

    private String callClaudeApi(Map<String, Object> request) {
        try {
            String response = restClient.post()
                .uri(API_URL)
                .header("x-api-key", claudeProperties.apiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

            return response;

        } catch (RestClientException e) {
            log.error("Claude API call failed: {}", e.getMessage());
            throw new ClaudeException("Claude API call failed", e);
        }
    }

    private String extractTextResponse(JsonNode content) {
        StringBuilder textBuilder = new StringBuilder();
        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText())) {
                textBuilder.append(block.path("text").asText());
            }
        }

        if (textBuilder.isEmpty()) {
            return "응답을 생성할 수 없습니다.";
        }
        return textBuilder.toString();
    }

    public static class ClaudeException extends RuntimeException {
        public ClaudeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
