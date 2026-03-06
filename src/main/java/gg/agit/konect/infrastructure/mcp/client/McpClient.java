package gg.agit.konect.infrastructure.mcp.client;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.infrastructure.mcp.config.McpProperties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class McpClient {

    private static final List<String> FORBIDDEN_KEYWORDS = List.of(
        "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", "TRUNCATE",
        "GRANT", "REVOKE", "EXEC", "EXECUTE", "INTO OUTFILE", "INTO DUMPFILE"
    );

    private final RestClient restClient;
    private final McpProperties mcpProperties;
    private final ObjectMapper objectMapper;

    public McpClient(RestClient.Builder restClientBuilder,
                     McpProperties mcpProperties,
                     ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.mcpProperties = mcpProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Execute a SELECT query via MCP bridge server.
     *
     * @param sql SQL query (SELECT only)
     * @return Query result as formatted string
     * @throws McpQueryException if query fails or is not read-only
     */
    public String executeQuery(String sql) {
        validateReadOnly(sql);

        try {
            String response = restClient.post()
                .uri(mcpProperties.url() + "/query")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("sql", sql))
                .retrieve()
                .body(String.class);

            return formatQueryResult(response);
        } catch (RestClientException e) {
            log.error("MCP query execution failed: {}", e.getMessage());
            throw new McpQueryException("Failed to execute query: " + e.getMessage(), e);
        }
    }

    /**
     * List all tables in the database.
     *
     * @return List of table names
     */
    public List<String> listTables() {
        try {
            String response = restClient.get()
                .uri(mcpProperties.url() + "/tables")
                .retrieve()
                .body(String.class);

            return parseTableList(response);
        } catch (RestClientException e) {
            log.error("Failed to list tables: {}", e.getMessage());
            throw new McpQueryException("Failed to list tables: " + e.getMessage(), e);
        }
    }

    /**
     * Get schema information for a specific table.
     *
     * @param tableName Name of the table
     * @return Table schema as formatted string
     */
    public String describeTable(String tableName) {
        try {
            String response = restClient.get()
                .uri(mcpProperties.url() + "/tables/" + tableName)
                .retrieve()
                .body(String.class);

            return formatTableDescription(response);
        } catch (RestClientException e) {
            log.error("Failed to describe table {}: {}", tableName, e.getMessage());
            throw new McpQueryException("Failed to describe table: " + e.getMessage(), e);
        }
    }

    /**
     * Check if MCP bridge server is healthy.
     *
     * @return true if healthy
     */
    public boolean isHealthy() {
        try {
            String response = restClient.get()
                .uri(mcpProperties.url() + "/health")
                .retrieve()
                .body(String.class);

            JsonNode node = objectMapper.readTree(response);
            return "healthy".equals(node.path("status").asText());
        } catch (Exception e) {
            log.warn("MCP health check failed: {}", e.getMessage());
            return false;
        }
    }

    private void validateReadOnly(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new McpQueryException("SQL query cannot be empty");
        }

        String normalizedSql = sql.trim().toUpperCase(Locale.ROOT);

        // Must start with SELECT
        if (!normalizedSql.startsWith("SELECT")) {
            throw new McpQueryException("Only SELECT queries are allowed");
        }

        // Check for forbidden keywords
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (normalizedSql.contains(keyword)) {
                throw new McpQueryException("Query contains forbidden keyword: " + keyword);
            }
        }
    }

    private String formatQueryResult(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            // MCP tools/call response structure
            JsonNode content = root.path("content");
            if (content.isArray() && !content.isEmpty()) {
                JsonNode firstContent = content.get(0);
                if (firstContent.has("text")) {
                    return firstContent.get("text").asText();
                }
            }

            // Direct result
            if (root.isArray()) {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            }

            return response;
        } catch (JsonProcessingException e) {
            log.warn("Failed to format query result, returning raw response");
            return response;
        }
    }

    private List<String> parseTableList(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            // Handle MCP response format
            JsonNode content = root.path("content");
            if (content.isArray() && !content.isEmpty()) {
                JsonNode firstContent = content.get(0);
                if (firstContent.has("text")) {
                    String text = firstContent.get("text").asText();
                    // Parse text as table list (assuming comma or newline separated)
                    return List.of(text.split("[,\\n]")).stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
                }
            }

            // Direct array response
            if (root.isArray()) {
                return objectMapper.convertValue(root, new TypeReference<List<String>>() { });
            }

            return List.of();
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse table list");
            return List.of();
        }
    }

    private String formatTableDescription(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            JsonNode content = root.path("content");
            if (content.isArray() && !content.isEmpty()) {
                JsonNode firstContent = content.get(0);
                if (firstContent.has("text")) {
                    return firstContent.get("text").asText();
                }
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (JsonProcessingException e) {
            return response;
        }
    }

    public static class McpQueryException extends RuntimeException {
        public McpQueryException(String message) {
            super(message);
        }

        public McpQueryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
