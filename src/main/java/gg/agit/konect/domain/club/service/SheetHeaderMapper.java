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
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
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
    private static final int MAX_TOKENS = 1024;
    private static final int SCAN_ROWS = 10;

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

    public record SheetAnalysisResult(
        SheetColumnMapping memberListMapping,
        Integer feeSheetId,
        SheetColumnMapping feeLedgerMapping
    ) {}

    public SheetAnalysisResult analyzeAllSheets(String spreadsheetId) {
        List<SheetInfo> sheets = readAllSheets(spreadsheetId);
        if (sheets.isEmpty()) {
            log.warn("No sheets found. Using default mapping.");
            return new SheetAnalysisResult(SheetColumnMapping.defaultMapping(), null, null);
        }

        try {
            return inferAllMappings(spreadsheetId, sheets);
        } catch (Exception e) {
            log.warn("Sheet analysis failed, using default. cause={}", e.getMessage());
            return new SheetAnalysisResult(SheetColumnMapping.defaultMapping(), null, null);
        }
    }

    private record SheetInfo(Integer sheetId, String title) {}

    private List<SheetInfo> readAllSheets(String spreadsheetId) {
        try {
            Spreadsheet spreadsheet = googleSheetsService.spreadsheets()
                .get(spreadsheetId)
                .execute();

            List<SheetInfo> result = new ArrayList<>();
            for (Sheet sheet : spreadsheet.getSheets()) {
                result.add(new SheetInfo(
                    sheet.getProperties().getSheetId(),
                    sheet.getProperties().getTitle()
                ));
            }
            return result;

        } catch (IOException e) {
            log.error("Failed to read spreadsheet info. spreadsheetId={}", spreadsheetId, e);
            return List.of();
        }
    }

    private List<List<String>> readSheetRows(String spreadsheetId, String sheetTitle) {
        try {
            String range = "'" + sheetTitle + "'!A1:Z10";
            ValueRange response = googleSheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
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
            log.warn("Failed to read rows from sheet '{}'. cause={}", sheetTitle, e.getMessage());
            return List.of();
        }
    }

    private SheetAnalysisResult inferAllMappings(
        String spreadsheetId,
        List<SheetInfo> sheets
    ) throws Exception {
        StringBuilder sheetsDescription = new StringBuilder();
        Map<String, List<List<String>>> sheetRowsMap = new HashMap<>();

        for (SheetInfo sheet : sheets) {
            List<List<String>> rows = readSheetRows(spreadsheetId, sheet.title());
            sheetRowsMap.put(sheet.title(), rows);

            sheetsDescription.append(String.format("=== Sheet: \"%s\" (sheetId: %d) ===%n",
                sheet.title(), sheet.sheetId()));
            if (rows.isEmpty()) {
                sheetsDescription.append("(empty)\n");
            } else {
                for (int i = 0; i < rows.size(); i++) {
                    sheetsDescription.append(String.format("Row %d: %s%n", i + 1, rows.get(i)));
                }
            }
            sheetsDescription.append("\n");
        }

        String prompt = buildPrompt(sheetsDescription.toString(), sheets);
        String rawJson = callClaude(prompt);
        return parseAllMappings(rawJson, sheets);
    }

    private String buildPrompt(String sheetsDescription, List<SheetInfo> sheets) {
        List<String> sheetNames = sheets.stream().map(SheetInfo::title).toList();
        return String.format("""
            A Korean university club uses a Google Spreadsheet with these sheets:
            %s

            %s

            Analyze the sheets and respond ONLY with a JSON object in this format:
            {
              "memberList": {
                "sheetTitle": "sheet name containing member list",
                "headerRow": 1,
                "mapping": {"name": 0, "studentId": 1, "email": 2}
              },
              "feeLedger": {
                "sheetTitle": "sheet name containing fee payment info, or null if none",
                "headerRow": 1,
                "mapping": {"name": 0, "feePaid": 1, "paidAt": 2}
              }
            }

            Field definitions:
            - memberList fields: name(이름/성명), studentId(학번), email(이메일),
              phone(전화번호/연락처), position(직책), joinedAt(가입일),
              feePaid(납부여부), paidAt(납부일)
            - feeLedger fields: name(이름/성명), feePaid(납부여부/회비),
              paidAt(납부일), studentId(학번)

            Rules:
            - "memberList.sheetTitle" must be one of: %s
            - "feeLedger.sheetTitle" must be one of: %s (or null if no fee-related sheet exists)
            - "headerRow" is 1-indexed
            - "mapping" uses 0-indexed column positions
            - Only include fields you are confident about
            - Do not include explanation
            """,
            sheetNames, sheetsDescription, sheetNames, sheetNames
        );
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

    private SheetAnalysisResult parseAllMappings(
        String rawJson,
        List<SheetInfo> sheets
    ) {
        try {
            String cleaned = rawJson.trim();
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start < 0 || end < 0) {
                throw new IllegalArgumentException("No JSON object found");
            }
            cleaned = cleaned.substring(start, end + 1);

            JsonNode root = objectMapper.readTree(cleaned);

            SheetColumnMapping memberListMapping = parseSingleMapping(root.path("memberList"));
            SheetColumnMapping feeLedgerMapping = null;
            Integer feeSheetId = null;

            JsonNode feeLedgerNode = root.path("feeLedger");
            if (!feeLedgerNode.isMissingNode() && !feeLedgerNode.isNull()) {
                String feeLedgerTitle = feeLedgerNode.path("sheetTitle").asText(null);
                if (feeLedgerTitle != null && !"null".equals(feeLedgerTitle)) {
                    feeLedgerMapping = parseSingleMapping(feeLedgerNode);
                    feeSheetId = sheets.stream()
                        .filter(s -> s.title().equals(feeLedgerTitle))
                        .map(SheetInfo::sheetId)
                        .findFirst()
                        .orElse(null);
                }
            }

            log.info(
                "Sheet analysis done. memberList={}, feeSheetId={}, feeLedger={}",
                memberListMapping.toMap(), feeSheetId,
                feeLedgerMapping != null ? feeLedgerMapping.toMap() : "none"
            );

            return new SheetAnalysisResult(memberListMapping, feeSheetId, feeLedgerMapping);

        } catch (Exception e) {
            log.warn("Failed to parse all mappings: {}. Using default.", rawJson);
            return new SheetAnalysisResult(SheetColumnMapping.defaultMapping(), null, null);
        }
    }

    private SheetColumnMapping parseSingleMapping(JsonNode node) {
        int headerRow = node.path("headerRow").asInt(1);
        int dataStartRow = headerRow + 1;

        JsonNode mappingNode = node.path("mapping");
        Map<String, Integer> mapping = new HashMap<>();
        mappingNode.fields().forEachRemaining(entry -> {
            int colIndex = entry.getValue().asInt(-1);
            if (colIndex >= 0) {
                mapping.put(entry.getKey(), colIndex);
            }
        });

        return new SheetColumnMapping(mapping, dataStartRow);
    }
}
