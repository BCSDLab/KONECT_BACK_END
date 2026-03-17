package gg.agit.konect.domain.club.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import gg.agit.konect.domain.club.model.SheetColumnMapping;
import gg.agit.konect.infrastructure.claude.config.ClaudeProperties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SheetHeaderMapper {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String MAPPING_MODEL = "claude-haiku-4-5-20251001";
    private static final int MAX_TOKENS = 256;
    private static final String HEADER_RANGE = "1:1";

    private final Sheets googleSheetsService;
    private final ClaudeProperties claudeProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public SheetHeaderMapper(
        Sheets googleSheetsService,
        ClaudeProperties claudeProperties,
        ObjectMapper objectMapper,
        RestClient.Builder restClientBuilder
    ) {
        this.googleSheetsService = googleSheetsService;
        this.claudeProperties = claudeProperties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    public SheetColumnMapping analyzeHeaders(String spreadsheetId) {
        List<String> headers = readHeaders(spreadsheetId);
        if (headers.isEmpty()) {
            log.warn("No headers found in spreadsheet. Using default mapping.");
            return SheetColumnMapping.defaultMapping();
        }

        try {
            return inferMapping(headers);
        } catch (Exception e) {
            log.warn(
                "Header analysis failed, using default mapping. cause={}",
                e.getMessage()
            );
            return SheetColumnMapping.defaultMapping();
        }
    }

    private List<String> readHeaders(String spreadsheetId) {
        try {
            ValueRange response = googleSheetsService.spreadsheets().values()
                .get(spreadsheetId, HEADER_RANGE)
                .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                return List.of();
            }

            return values.get(0).stream()
                .map(Object::toString)
                .toList();

        } catch (IOException e) {
            log.error("Failed to read headers. spreadsheetId={}", spreadsheetId, e);
            return List.of();
        }
    }

    private SheetColumnMapping inferMapping(List<String> headers) throws Exception {
        String prompt = buildPrompt(headers);
        String rawJson = callClaude(prompt);
        return parseMapping(rawJson, headers.size());
    }

    private String buildPrompt(List<String> headers) {
        return String.format("""
            The following are column headers from a spreadsheet used by a Korean university club:
            %s

            Map each header to one of these field names if it matches. Column index starts at 0.
            Fields: name, studentId, email, phone, position, joinedAt, feePaid, paidAt

            Rules:
            - "name" = member's name (이름, 성명, 이름 등)
            - "studentId" = student number (학번, 학생번호 등)
            - "email" = email address (이메일, 이메일주소 등)
            - "phone" = phone number (전화번호, 연락처, 핸드폰 등)
            - "position" = role/position in club (직책, 직급, 역할 등)
            - "joinedAt" = join date (가입일, 가입날짜, 입부일 등)
            - "feePaid" = fee payment status (회비, 납부여부, 납부, 회비납부 등)
            - "paidAt" = fee payment date (납부일, 납부날짜 등)

            Respond ONLY with a JSON object like:
            {"name": 0, "studentId": 1, "email": 2}
            Only include fields you are confident about. Do not include explanation.
            """, headers);
    }

    private String callClaude(String prompt) {
        Map<String, Object> request = Map.of(
            "model", MAPPING_MODEL,
            "max_tokens", MAX_TOKENS,
            "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            String response = restClient.post()
                .uri(API_URL)
                .header("x-api-key", claudeProperties.apiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("content").get(0).path("text").asText();

        } catch (RestClientException | IOException e) {
            throw new RuntimeException("Claude API call failed", e);
        }
    }

    private SheetColumnMapping parseMapping(String rawJson, int headerCount) {
        try {
            String cleaned = rawJson.trim();
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start < 0 || end < 0) {
                throw new IllegalArgumentException("No JSON object found in response");
            }
            cleaned = cleaned.substring(start, end + 1);

            JsonNode node = objectMapper.readTree(cleaned);
            Map<String, Integer> mapping = new HashMap<>();

            node.fields().forEachRemaining(entry -> {
                int colIndex = entry.getValue().asInt(-1);
                if (colIndex >= 0 && colIndex < headerCount) {
                    mapping.put(entry.getKey(), colIndex);
                }
            });

            log.info("Sheet header mapping resolved: {}", mapping);
            return new SheetColumnMapping(mapping);

        } catch (Exception e) {
            log.warn("Failed to parse mapping JSON: {}. Using default.", rawJson);
            return SheetColumnMapping.defaultMapping();
        }
    }
}
