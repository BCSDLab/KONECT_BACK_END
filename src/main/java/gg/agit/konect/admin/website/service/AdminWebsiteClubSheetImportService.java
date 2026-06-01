package gg.agit.konect.admin.website.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportConfirmRequest;
import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportPreviewResponse;
import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportResponse;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.service.GoogleSheetApiExceptionHelper;
import gg.agit.konect.domain.club.service.SpreadsheetUrlParser;
import gg.agit.konect.domain.website.model.WebClub;
import gg.agit.konect.domain.website.model.WebUniversity;
import gg.agit.konect.domain.website.repository.WebClubRepository;
import gg.agit.konect.domain.website.repository.WebUniversityRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.infrastructure.claude.config.ClaudeProperties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AdminWebsiteClubSheetImportService {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String MODEL = "claude-haiku-4-5-20251001";
    private static final String INPUT_SHEET_RANGE = "'작성 시트'!A1:F1000";
    private static final int MAX_TOKENS = 4096;
    private static final int DEFAULT_HEADER_INDEX = 3;
    private static final int NAME_MAX_LENGTH = 50;
    private static final int TOPIC_MAX_LENGTH = 20;
    private static final int DESCRIPTION_MAX_LENGTH = 30;
    private static final int CATEGORY_EMOJI_MAX_LENGTH = 255;
    private static final int NAME_COLUMN_INDEX = 0;
    private static final int CATEGORY_COLUMN_INDEX = 1;
    private static final int CUSTOM_CATEGORY_COLUMN_INDEX = 2;
    private static final int TOPIC_COLUMN_INDEX = 3;
    private static final int CATEGORY_EMOJI_COLUMN_INDEX = 4;
    private static final int DESCRIPTION_COLUMN_INDEX = 5;

    private final Sheets googleSheetsService;
    private final ClaudeProperties claudeProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final WebUniversityRepository webUniversityRepository;
    private final WebClubRepository webClubRepository;

    public AdminWebsiteClubSheetImportService(
        Sheets googleSheetsService,
        ClaudeProperties claudeProperties,
        ObjectMapper objectMapper,
        RestClient.Builder restClientBuilder,
        WebUniversityRepository webUniversityRepository,
        WebClubRepository webClubRepository
    ) {
        this.googleSheetsService = googleSheetsService;
        this.claudeProperties = claudeProperties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
        this.webUniversityRepository = webUniversityRepository;
        this.webClubRepository = webClubRepository;
    }

    public AdminWebsiteClubSheetImportPreviewResponse previewClubs(
        Integer universityId,
        String spreadsheetUrl
    ) {
        webUniversityRepository.getById(universityId);
        String spreadsheetId = SpreadsheetUrlParser.extractId(spreadsheetUrl);
        List<RawClubRow> rows = readClubRows(spreadsheetId);
        AnalyzedClubSheet analyzedSheet = analyzeRowsWithClaude(rows);
        return AdminWebsiteClubSheetImportPreviewResponse.of(
            universityId,
            analyzedSheet.clubs(),
            analyzedSheet.warnings()
        );
    }

    @Transactional
    public AdminWebsiteClubSheetImportResponse confirmImport(
        Integer universityId,
        List<AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub> clubs
    ) {
        WebUniversity university = webUniversityRepository.getById(universityId);
        List<String> warnings = new ArrayList<>();
        List<AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub> enabledClubs = clubs == null
            ? List.of()
            : clubs.stream()
                .filter(AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub::enabled)
                .toList();

        Set<String> requestNames = new LinkedHashSet<>();
        enabledClubs.stream()
            .map(AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub::name)
            .map(String::trim)
            .filter(name -> !name.isBlank())
            .forEach(requestNames::add);

        Set<String> existingNames = requestNames.isEmpty()
            ? Set.of()
            : webClubRepository.findExistingNamesByUniversityId(universityId, requestNames);

        Set<String> seenNames = new LinkedHashSet<>();
        List<WebClub> clubsToSave = new ArrayList<>();

        for (AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub club : enabledClubs) {
            String name = club.name().trim();
            String normalizedName = name.toLowerCase(Locale.ROOT);
            if (name.isBlank()) {
                warnings.add(String.format("%d행: 동아리명이 비어 있어 제외했습니다.", club.rowNumber()));
                continue;
            }
            if (!seenNames.add(normalizedName)) {
                warnings.add(String.format("%d행: 요청 안에서 중복된 동아리명 '%s'을 제외했습니다.", club.rowNumber(), name));
                continue;
            }
            if (existingNames.contains(name)) {
                warnings.add(String.format("%d행: 이미 등록된 동아리명 '%s'을 제외했습니다.", club.rowNumber(), name));
                continue;
            }

            clubsToSave.add(WebClub.builder()
                .university(university)
                .clubCategory(club.clubCategory())
                .name(name)
                .topic(limit(requiredText(club.topic(), "기타"), TOPIC_MAX_LENGTH))
                .description(limit(requiredText(club.description(), name), DESCRIPTION_MAX_LENGTH))
                .introduce(requiredText(club.introduce(), club.description()))
                .categoryEmoji(limit(
                    requiredText(club.categoryEmoji(), emojiOf(club.clubCategory())),
                    CATEGORY_EMOJI_MAX_LENGTH
                ))
                .build());
        }

        List<WebClub> savedClubs = clubsToSave.isEmpty()
            ? List.of()
            : webClubRepository.saveAll(clubsToSave);

        return AdminWebsiteClubSheetImportResponse.of(
            savedClubs.size(),
            enabledClubs.size() - savedClubs.size(),
            warnings
        );
    }

    private List<RawClubRow> readClubRows(String spreadsheetId) {
        try {
            ValueRange response = googleSheetsService.spreadsheets().values()
                .get(spreadsheetId, INPUT_SHEET_RANGE)
                .setValueRenderOption("FORMATTED_VALUE")
                .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                return List.of();
            }

            int headerIndex = findHeaderIndex(values);
            List<RawClubRow> rows = new ArrayList<>();
            for (int rowIndex = headerIndex + 1; rowIndex < values.size(); rowIndex++) {
                List<Object> row = values.get(rowIndex);
                RawClubRow rawClubRow = new RawClubRow(
                    rowIndex + 1,
                    cell(row, NAME_COLUMN_INDEX),
                    cell(row, CATEGORY_COLUMN_INDEX),
                    cell(row, CUSTOM_CATEGORY_COLUMN_INDEX),
                    cell(row, TOPIC_COLUMN_INDEX),
                    cell(row, CATEGORY_EMOJI_COLUMN_INDEX),
                    cell(row, DESCRIPTION_COLUMN_INDEX)
                );
                if (!rawClubRow.isEmpty()) {
                    rows.add(rawClubRow);
                }
            }
            return rows;
        } catch (IOException e) {
            if (GoogleSheetApiExceptionHelper.isAccessDenied(e)) {
                throw GoogleSheetApiExceptionHelper.accessDenied();
            }
            log.error("Failed to read website club import sheet. spreadsheetId={}", spreadsheetId, e);
            throw CustomException.of(ApiResponseCode.FAILED_SYNC_GOOGLE_SHEET);
        }
    }

    private int findHeaderIndex(List<List<Object>> values) {
        for (int i = 0; i < values.size(); i++) {
            if ("동아리명".equals(cell(values.get(i), 0))) {
                return i;
            }
        }
        return DEFAULT_HEADER_INDEX;
    }

    private AnalyzedClubSheet analyzeRowsWithClaude(List<RawClubRow> rows) {
        if (rows.isEmpty()) {
            return new AnalyzedClubSheet(List.of(), List.of("분석할 동아리 행이 없습니다."));
        }

        String prompt = buildPrompt(rows);
        Map<String, Object> request = Map.of(
            "model", MODEL,
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
            String rawJson = root.path("content").get(0).path("text").asText();
            return parseClaudeResult(rawJson);
        } catch (Exception e) {
            log.error("Failed to analyze website club import sheet with Claude.", e);
            throw CustomException.of(ApiResponseCode.FAILED_SYNC_GOOGLE_SHEET);
        }
    }

    private String buildPrompt(List<RawClubRow> rows) {
        try {
            String rowsJson = objectMapper.writeValueAsString(rows);
            return """
                You normalize Korean university club registration sheet rows for KONECT.

                Respond ONLY with JSON:
                {
                  "clubs": [
                    {
                      "rowNumber": 5,
                      "name": "BCSD",
                      "clubCategory": "ACADEMIC",
                      "topic": "개발",
                      "description": "30자 이내 한 줄 소개",
                      "introduce": "서비스에 표시할 상세 소개",
                      "categoryEmoji": "💻"
                    }
                  ],
                  "warnings": ["row-specific warning in Korean"]
                }

                Club category enum mapping:
                - 공연분과 -> PERFORMANCE
                - 사회/봉사분과 -> SOCIAL_SERVICE
                - 전시/창작분과 -> EXHIBITION_CREATION
                - 종교분과 -> RELIGION
                - 체육(운동)분과 -> SPORTS
                - 취미분과 -> HOBBY
                - 학술분과 -> ACADEMIC
                - 기타분과, 기타, unknown -> ETC

                Rules:
                - Skip example rows and blank rows.
                - Keep name within 50 characters.
                - Keep topic within 20 characters.
                - Keep description within 30 Korean characters. If blank, create one from name/topic/category.
                - introduce is required. If no detailed content is available, reuse description.
                - categoryEmoji is required. If blank or unsuitable, choose one relevant emoji.
                - Add warnings for missing or corrected important fields.

                Rows:
                %s
                """.formatted(rowsJson);
        } catch (IOException e) {
            throw CustomException.of(ApiResponseCode.FAILED_SYNC_GOOGLE_SHEET);
        }
    }

    private AnalyzedClubSheet parseClaudeResult(String rawJson) throws IOException {
        String cleaned = extractJsonObject(rawJson);
        JsonNode root = objectMapper.readTree(cleaned);
        List<AdminWebsiteClubSheetImportPreviewResponse.PreviewClub> clubs = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (JsonNode warning : root.path("warnings")) {
            warnings.add(warning.asText());
        }

        for (JsonNode clubNode : root.path("clubs")) {
            String name = limit(requiredText(clubNode.path("name").asText(), ""), NAME_MAX_LENGTH);
            if (name.isBlank() || name.startsWith("예시)")) {
                continue;
            }

            ClubCategory category = resolveCategory(clubNode.path("clubCategory").asText());
            String topic = limit(requiredText(clubNode.path("topic").asText(), "기타"), TOPIC_MAX_LENGTH);
            String description = limit(
                requiredText(clubNode.path("description").asText(), name + " 동아리입니다."),
                DESCRIPTION_MAX_LENGTH
            );
            String introduce = requiredText(clubNode.path("introduce").asText(), description);
            String categoryEmoji = limit(
                requiredText(clubNode.path("categoryEmoji").asText(), emojiOf(category)),
                CATEGORY_EMOJI_MAX_LENGTH
            );

            clubs.add(new AdminWebsiteClubSheetImportPreviewResponse.PreviewClub(
                clubNode.path("rowNumber").asInt(0),
                name,
                category,
                topic,
                description,
                introduce,
                categoryEmoji,
                true
            ));
        }

        return new AnalyzedClubSheet(clubs, warnings);
    }

    private ClubCategory resolveCategory(String value) {
        if (value == null || value.isBlank()) {
            return ClubCategory.ETC;
        }
        String normalized = value.trim();
        for (ClubCategory category : ClubCategory.values()) {
            if (category.name().equalsIgnoreCase(normalized)) {
                return category;
            }
        }
        return switch (normalized) {
            case "공연분과" -> ClubCategory.PERFORMANCE;
            case "사회/봉사분과" -> ClubCategory.SOCIAL_SERVICE;
            case "전시/창작분과" -> ClubCategory.EXHIBITION_CREATION;
            case "종교분과" -> ClubCategory.RELIGION;
            case "체육(운동)분과" -> ClubCategory.SPORTS;
            case "취미분과" -> ClubCategory.HOBBY;
            case "학술분과" -> ClubCategory.ACADEMIC;
            default -> ClubCategory.ETC;
        };
    }

    private static String cell(List<Object> row, int index) {
        if (row == null || index >= row.size() || row.get(index) == null) {
            return "";
        }
        return row.get(index).toString().trim();
    }

    private static String requiredText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static String extractJsonObject(String rawJson) {
        String cleaned = rawJson == null ? "" : rawJson.trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("No JSON object found");
        }
        return cleaned.substring(start, end + 1);
    }

    private static String emojiOf(ClubCategory category) {
        return switch (category) {
            case PERFORMANCE -> "🎭";
            case SOCIAL_SERVICE -> "🤝";
            case EXHIBITION_CREATION -> "🎨";
            case RELIGION -> "🙏";
            case SPORTS -> "⚽";
            case HOBBY -> "📷";
            case ACADEMIC -> "📚";
            case ETC -> "✨";
        };
    }

    private record RawClubRow(
        int rowNumber,
        String name,
        String category,
        String customCategory,
        String topic,
        String categoryEmoji,
        String description
    ) {
        private boolean isEmpty() {
            return name.isBlank()
                && category.isBlank()
                && customCategory.isBlank()
                && topic.isBlank()
                && categoryEmoji.isBlank()
                && description.isBlank();
        }
    }

    private record AnalyzedClubSheet(
        List<AdminWebsiteClubSheetImportPreviewResponse.PreviewClub> clubs,
        List<String> warnings
    ) {
    }
}
