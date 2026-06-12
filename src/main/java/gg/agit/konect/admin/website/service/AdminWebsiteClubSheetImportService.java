package gg.agit.konect.admin.website.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
import gg.agit.konect.domain.website.service.WebsiteClubStatsReader;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminWebsiteClubSheetImportService {

    private static final String INPUT_SHEET_RANGE = "'작성 시트'!A1:F1000";
    private static final String HEADER_NAME = "동아리명";
    private static final String EXAMPLE_PREFIX = "예시)";
    private static final int DEFAULT_HEADER_INDEX = 3;
    private static final int NAME_MAX_LENGTH = 50;
    private static final int TOPIC_MAX_LENGTH = 20;
    private static final int DESCRIPTION_MAX_LENGTH = 30;
    private static final int CATEGORY_EMOJI_MAX_LENGTH = 255;
    private static final String EMPTY_INTRODUCE = "";
    private static final int NAME_COLUMN_INDEX = 0;
    private static final int CATEGORY_COLUMN_INDEX = 1;
    private static final int CUSTOM_CATEGORY_COLUMN_INDEX = 2;
    private static final int TOPIC_COLUMN_INDEX = 3;
    private static final int CATEGORY_EMOJI_COLUMN_INDEX = 4;
    private static final int DESCRIPTION_COLUMN_INDEX = 5;
    private static final Set<String> PLACEHOLDER_TEXTS = Set.of(
        "-",
        "없음",
        "미정",
        "미확인",
        "확인필요",
        "확인 필요",
        "조사필요",
        "조사 필요",
        "미분류"
    );
    private static final Pattern URL_OR_SNS_PATTERN = Pattern.compile(
        "(?i).*(https?://|www\\.|instagram\\.com|open\\.kakao|kakao\\.com|@\\w+).*"
    );
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile(".*\\d{2,3}[- .]?\\d{3,4}[- .]?\\d{4}.*");

    private final Sheets googleSheetsService;
    private final WebUniversityRepository webUniversityRepository;
    private final WebClubRepository webClubRepository;
    private final WebsiteClubStatsReader websiteClubStatsReader;

    public AdminWebsiteClubSheetImportPreviewResponse previewClubs(
        Integer universityId,
        String spreadsheetUrl
    ) {
        webUniversityRepository.getById(universityId);
        String spreadsheetId = SpreadsheetUrlParser.extractId(spreadsheetUrl);
        SheetClubImportPlan importPlan = buildImportPlan(readClubRows(spreadsheetId));
        return AdminWebsiteClubSheetImportPreviewResponse.of(
            universityId,
            importPlan.clubs(),
            importPlan.warnings()
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
        Set<String> normalizedExistingNames = existingNames.stream()
            .map(name -> name.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());

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
            if (normalizedExistingNames.contains(normalizedName)) {
                warnings.add(String.format("%d행: 이미 등록된 동아리명 '%s'을 제외했습니다.", club.rowNumber(), name));
                continue;
            }
            List<String> contentWarnings = validateClubContent(
                club.rowNumber(),
                name,
                club.topic(),
                club.description()
            );
            if (!contentWarnings.isEmpty()) {
                warnings.addAll(contentWarnings);
                continue;
            }

            clubsToSave.add(WebClub.builder()
                .university(university)
                .clubCategory(club.clubCategory())
                .name(name)
                .topic(limit(requiredText(club.topic(), "기타"), TOPIC_MAX_LENGTH))
                .description(limit(requiredText(club.description(), name), DESCRIPTION_MAX_LENGTH))
                .introduce(optionalText(club.introduce()))
                .categoryEmoji(limit(
                    requiredText(club.categoryEmoji(), emojiOf(club.clubCategory())),
                    CATEGORY_EMOJI_MAX_LENGTH
                ))
                .build());
        }

        List<WebClub> savedClubs = clubsToSave.isEmpty()
            ? List.of()
            : webClubRepository.saveAll(clubsToSave);
        if (!savedClubs.isEmpty()) {
            invalidateWebsiteStatsAfterCommit(universityId);
        }

        return AdminWebsiteClubSheetImportResponse.of(
            savedClubs.size(),
            enabledClubs.size() - savedClubs.size(),
            warnings
        );
    }

    private SheetClubImportPlan buildImportPlan(List<RawClubRow> rows) {
        List<AdminWebsiteClubSheetImportPreviewResponse.PreviewClub> clubs = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (RawClubRow row : rows) {
            String name = limit(row.name(), NAME_MAX_LENGTH);
            if (name.isBlank() || name.startsWith(EXAMPLE_PREFIX)) {
                continue;
            }

            ClubCategory category = resolveCategory(row.category(), row.customCategory());
            String topic = limit(requiredText(row.topic(), "기타"), TOPIC_MAX_LENGTH);
            String categoryEmoji = limit(
                requiredText(row.categoryEmoji(), emojiOf(category)),
                CATEGORY_EMOJI_MAX_LENGTH
            );
            String description = limit(
                requiredText(row.description(), name + " 동아리입니다."),
                DESCRIPTION_MAX_LENGTH
            );
            List<String> contentWarnings = validateClubContent(
                row.rowNumber(),
                row.name(),
                row.topic(),
                row.description()
            );

            addWarnings(row, category, topic, categoryEmoji, description, warnings);
            warnings.addAll(contentWarnings);
            clubs.add(new AdminWebsiteClubSheetImportPreviewResponse.PreviewClub(
                row.rowNumber(),
                name,
                category,
                topic,
                description,
                EMPTY_INTRODUCE,
                categoryEmoji,
                contentWarnings.isEmpty()
            ));
        }

        if (clubs.isEmpty()) {
            warnings.add("가져올 동아리 행이 없습니다.");
        }
        return new SheetClubImportPlan(clubs, warnings);
    }

    private void addWarnings(
        RawClubRow row,
        ClubCategory category,
        String topic,
        String categoryEmoji,
        String description,
        List<String> warnings
    ) {
        if (row.category().isBlank()) {
            warnings.add(String.format("%d행: 동아리 분과가 비어 있어 기타로 처리했습니다.", row.rowNumber()));
        }
        if (category == ClubCategory.ETC && !row.category().isBlank() && row.customCategory().isBlank()) {
            warnings.add(String.format("%d행: 기타 분과 상세 내용이 비어 있습니다.", row.rowNumber()));
        }
        if (row.topic().isBlank()) {
            warnings.add(String.format("%d행: 동아리 주제가 비어 있어 '%s'(으)로 처리했습니다.", row.rowNumber(), topic));
        }
        if (row.categoryEmoji().isBlank()) {
            warnings.add(String.format("%d행: 대표 이모지가 비어 있어 '%s'(으)로 처리했습니다.", row.rowNumber(), categoryEmoji));
        }
        if (row.description().isBlank()) {
            warnings.add(String.format("%d행: 한 줄 소개가 비어 있어 '%s'(으)로 처리했습니다.", row.rowNumber(), description));
        }
    }

    private void invalidateWebsiteStatsAfterCommit(Integer universityId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            websiteClubStatsReader.invalidateUniversity(universityId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                websiteClubStatsReader.invalidateUniversity(universityId);
            }
        });
    }

    private List<String> validateClubContent(
        int rowNumber,
        String name,
        String topic,
        String description
    ) {
        List<String> warnings = new ArrayList<>();
        String normalizedName = optionalText(name);
        if (isSuspiciousName(normalizedName)) {
            warnings.add(String.format("%d행: 동아리명이 소개 문장 또는 시트 헤더처럼 보여 제외했습니다.", rowNumber));
        }
        if (isSuspiciousShortText(topic)) {
            warnings.add(String.format("%d행: 동아리 주제에 미확인/연락처성 문구가 있어 제외했습니다.", rowNumber));
        }
        if (isSuspiciousShortText(description)) {
            warnings.add(String.format("%d행: 한 줄 소개에 미확인/연락처성 문구가 있어 제외했습니다.", rowNumber));
        }
        return warnings;
    }

    private boolean isSuspiciousName(String name) {
        if (name.isBlank()) {
            return false;
        }
        String normalized = name.trim();
        // Header/label fragments mean a sheet row or intro column leaked into the name field.
        return HEADER_NAME.equals(normalized)
            || normalized.contains("한 줄 소개")
            || normalized.contains("상세소개")
            // Sentence-like names usually came from one-line introductions rather than club names.
            || normalized.contains("동아리입니다")
            || normalized.endsWith("입니다.")
            || normalized.endsWith("합니다.")
            // Contact handles and placeholders are not stable display names.
            || URL_OR_SNS_PATTERN.matcher(normalized).matches()
            || PHONE_NUMBER_PATTERN.matcher(normalized).matches()
            || isPlaceholder(normalized);
    }

    private boolean isSuspiciousShortText(String value) {
        String normalized = optionalText(value);
        if (normalized.isBlank()) {
            return false;
        }
        return isPlaceholder(normalized)
            || URL_OR_SNS_PATTERN.matcher(normalized).matches()
            || PHONE_NUMBER_PATTERN.matcher(normalized).matches();
    }

    private boolean isPlaceholder(String value) {
        return PLACEHOLDER_TEXTS.contains(value.trim().toLowerCase(Locale.ROOT));
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
            if (HEADER_NAME.equals(cell(values.get(i), NAME_COLUMN_INDEX))) {
                return i;
            }
        }
        return DEFAULT_HEADER_INDEX;
    }

    private ClubCategory resolveCategory(String categoryText, String customCategoryText) {
        String normalized = requiredText(categoryText, customCategoryText);
        if (normalized.isBlank()) {
            return ClubCategory.ETC;
        }
        for (ClubCategory category : ClubCategory.values()) {
            if (category.name().equalsIgnoreCase(normalized)) {
                return category;
            }
        }
        return switch (normalized.trim()) {
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

    private static String optionalText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
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

    private record SheetClubImportPlan(
        List<AdminWebsiteClubSheetImportPreviewResponse.PreviewClub> clubs,
        List<String> warnings
    ) {
    }
}
