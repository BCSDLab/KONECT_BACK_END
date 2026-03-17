package gg.agit.konect.domain.club.service;

import java.io.IOException;
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
    private static final int MAX_TOKENS = 512;
    private static final int SCAN_ROWS = 10;
    private static final String SCAN_RANGE = "A1:Z10";

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
        List<List<String>> rows = readRows(spreadsheetId);
        if (rows.isEmpty()) {
            log.warn("No data found in spreadsheet. Using default mapping.");
            return SheetColumnMapping.defaultMapping();
        }

        try {
            return inferMapping(rows);
        } catch (Exception e) {
            log.warn(
                "Header analysis failed, using default mapping. cause={}",
                e.getMessage()
            );
            return SheetColumnMapping.defaultMapping();
        }
    }

    private List<List<String>> readRows(String spreadsheetId) {
        try {
            ValueRange response = googleSheetsService.spreadsheets().values()
                .get(spreadsheetId, SCAN_RANGE)
                .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                return List.of();
            }

            List<List<String>> rows = new ArrayList<>();
            int limit = Math.min(values.size(), SCAN_ROWS);
            for (int i = 0; i < limit; i++) {
                List<String> row = values.get(i).stream()
                    .map(Object::toString)
                    .toList();
                rows.add(row);
            }
            return rows;

        } catch (IOException e) {
            log.error("Failed to read rows. spreadsheetId={}", spreadsheetId, e);
            return List.of();
        }
    }

    private SheetColumnMapping inferMapping(List<List<String>> rows) throws Exception {
        String prompt = buildPrompt(rows);
        String rawJson = callClaude(prompt);
        return parseMapping(rawJson);
    }

    private String buildPrompt(List<List<String>> rows) {
        StringBuilder rowsDescription = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            rowsDescription.append(String.format("Row %d: %s%n", i + 1, rows.get(i)));
        }

        return String.format("""
            Below are the first rows of a spreadsheet used by a Korean university club:

            %s
            First, identify which row contains the column headers (not title or blank rows).
            Then, map each header to one of these field names. Column index starts at 0.
            Fields: name, studentId, email, phone, position, joinedAt, feePaid, paidAt

            Rules:
            - "name" = member's name (이름, 성명 etc.)
            - "studentId" = student number (학번, 학생번호 etc.)
            - "email" = email address (이메일, 이메일주소 etc.)
            - "phone" = phone number (전화번호, 연락처, 핸드폰 etc.)
            - "position" = role in club (직책, 직급, 역할 etc.)
            - "joinedAt" = join date (가입일, 가입날짜, 입부일 etc.)
            - "feePaid" = fee payment status (회비, 납부여부, 회비납부 etc.)
            - "paidAt" = fee payment date (납부일, 납부날짜 etc.)

            Respond ONLY with a JSON object in this exact format:
            {"headerRow": 1, "mapping": {"name": 0, "studentId": 1}}

            - "headerRow" is the 1-indexed row number of the header row.
            - "mapping" contains only fields you are confident about.
            - Do not include any explanation.
            """, rowsDescription);
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

    private SheetColumnMapping parseMapping(String rawJson) {
        try {
            String cleaned = rawJson.trim();
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start < 0 || end < 0) {
                throw new IllegalArgumentException("No JSON object found in response");
            }
            cleaned = cleaned.substring(start, end + 1);

            JsonNode root = objectMapper.readTree(cleaned);
            int headerRow = root.path("headerRow").asInt(1);
            int dataStartRow = headerRow + 1;

            JsonNode mappingNode = root.path("mapping");
            Map<String, Integer> mapping = new HashMap<>();

            mappingNode.fields().forEachRemaining(entry -> {
                int colIndex = entry.getValue().asInt(-1);
                if (colIndex >= 0) {
                    mapping.put(entry.getKey(), colIndex);
                }
            });

            log.info(
                "Sheet header mapping resolved. headerRow={}, dataStartRow={}, mapping={}",
                headerRow, dataStartRow, mapping
            );
            return new SheetColumnMapping(mapping, dataStartRow);

        } catch (Exception e) {
            log.warn("Failed to parse mapping JSON: {}. Using default.", rawJson);
            return SheetColumnMapping.defaultMapping();
        }
    }
}
