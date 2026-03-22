package gg.agit.konect.domain.club.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.SheetColumnMapping;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.UserOAuthAccount;
import gg.agit.konect.domain.user.repository.UserOAuthAccountRepository;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.infrastructure.googlesheets.GoogleSheetsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SheetMigrationService {

    private static final Pattern FOLDER_ID_PATTERN =
        Pattern.compile("(?:folders/|id=)([a-zA-Z0-9_-]{20,})");
    private static final Pattern SPREADSHEET_ID_PATTERN =
        Pattern.compile("/spreadsheets/d/([a-zA-Z0-9_-]+)");
    private static final String MIME_TYPE_SPREADSHEET =
        "application/vnd.google-apps.spreadsheet";
    private static final String NEW_SHEET_TITLE_PREFIX = "KONECT_인명부_";

    @Value("${google.sheets.template-spreadsheet-id:}")
    private String defaultTemplateSpreadsheetId;

    private final Sheets googleSheetsService;
    private final SheetHeaderMapper sheetHeaderMapper;
    private final ClubRepository clubRepository;
    private final UserOAuthAccountRepository userOAuthAccountRepository;
    private final ClubPermissionValidator clubPermissionValidator;
    private final GoogleSheetsConfig googleSheetsConfig;
    private final ObjectMapper objectMapper;

    @Transactional
    public String migrateToTemplate(
        Integer clubId,
        Integer requesterId,
        String sourceSpreadsheetUrl
    ) {
        Club club = clubRepository.getById(clubId);
        clubPermissionValidator.validateManagerAccess(clubId, requesterId);

        String templateId = defaultTemplateSpreadsheetId;
        if (templateId == null || templateId.isBlank()) {
            throw CustomException.of(ApiResponseCode.NOT_FOUND_CLUB_SHEET_ID);
        }

        UserOAuthAccount oauthAccount = userOAuthAccountRepository
            .findByUserIdAndProvider(requesterId, Provider.GOOGLE)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_GOOGLE_DRIVE_AUTH));

        String driveRefreshToken = oauthAccount.getGoogleDriveRefreshToken();
        if (driveRefreshToken == null || driveRefreshToken.isBlank()) {
            throw CustomException.of(ApiResponseCode.NOT_FOUND_GOOGLE_DRIVE_AUTH);
        }

        Drive userDriveService;
        try {
            userDriveService = googleSheetsConfig.buildUserDriveService(driveRefreshToken);
        } catch (IOException | GeneralSecurityException e) {
            log.error("Failed to build user Drive service. requesterId={}", requesterId, e);
            throw new RuntimeException("Failed to initialize Google Drive with user credentials", e);
        }

        String sourceSpreadsheetId = extractSpreadsheetId(sourceSpreadsheetUrl);
        String folderId = resolveFolderId(userDriveService, sourceSpreadsheetUrl, sourceSpreadsheetId);

        String newSpreadsheetId = copyTemplate(userDriveService, templateId, club.getName(), folderId);

        SheetHeaderMapper.SheetAnalysisResult sourceAnalysis =
            sheetHeaderMapper.analyzeAllSheets(sourceSpreadsheetId);

        List<List<Object>> sourceData = readAllData(
            sourceSpreadsheetId,
            sourceAnalysis.memberListMapping()
        );

        writeToTemplate(newSpreadsheetId, sourceData, sourceAnalysis.memberListMapping());

        club.updateGoogleSheetId(newSpreadsheetId);
        if (folderId != null) {
            club.updateDriveFolderId(folderId);
        }

        SheetHeaderMapper.SheetAnalysisResult newAnalysis =
            sheetHeaderMapper.analyzeAllSheets(newSpreadsheetId);
        try {
            club.updateSheetColumnMapping(
                objectMapper.writeValueAsString(newAnalysis.memberListMapping().toMap())
            );
        } catch (Exception e) {
            log.warn("Failed to serialize new mapping. cause={}", e.getMessage());
        }

        log.info(
            "Sheet migration done. clubId={}, sourceId={}, newId={}, folderId={}",
            clubId, sourceSpreadsheetId, newSpreadsheetId, folderId
        );

        return newSpreadsheetId;
    }

    private String extractSpreadsheetId(String url) {
        Matcher m = SPREADSHEET_ID_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return url;
    }

    private String resolveFolderId(Drive driveService, String url, String spreadsheetId) {
        Matcher m = FOLDER_ID_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        try {
            File file = driveService.files().get(spreadsheetId)
                .setFields("parents")
                .execute();
            List<String> parents = file.getParents();
            if (parents != null && !parents.isEmpty()) {
                return parents.get(0);
            }
        } catch (IOException e) {
            log.warn("Failed to get parent folder of spreadsheet. cause={}", e.getMessage());
        }
        return null;
    }

    private String copyTemplate(Drive driveService, String templateId, String clubName, String targetFolderId) {
        try {
            String title = NEW_SHEET_TITLE_PREFIX + clubName;
            File copyMetadata = new File().setName(title);

            if (targetFolderId != null) {
                copyMetadata.setParents(Collections.singletonList(targetFolderId));
            }

            File copied = driveService.files().copy(templateId, copyMetadata)
                .setFields("id")
                .execute();

            log.info("Template copied by user. newId={}, folderId={}", copied.getId(), targetFolderId);
            return copied.getId();

        } catch (IOException e) {
            log.error("Failed to copy template. cause={}", e.getMessage(), e);
            throw new RuntimeException("Failed to copy template spreadsheet", e);
        }
    }

    private List<List<Object>> readAllData(
        String spreadsheetId,
        SheetColumnMapping mapping
    ) {
        try {
            int dataStartRow = mapping.getDataStartRow();
            String range = "A" + dataStartRow + ":Z";
            ValueRange response = googleSheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

            List<List<Object>> values = response.getValues();
            return values != null ? values : List.of();

        } catch (IOException e) {
            log.error("Failed to read source data. cause={}", e.getMessage(), e);
            return List.of();
        }
    }

    private void writeToTemplate(
        String newSpreadsheetId,
        List<List<Object>> sourceData,
        SheetColumnMapping sourceMapping
    ) {
        if (sourceData.isEmpty()) {
            return;
        }

        try {
            SheetHeaderMapper.SheetAnalysisResult templateAnalysis =
                sheetHeaderMapper.analyzeAllSheets(newSpreadsheetId);
            SheetColumnMapping targetMapping = templateAnalysis.memberListMapping();
            int targetDataStartRow = targetMapping.getDataStartRow();

            Map<String, Integer> sourceFieldToCol = buildReverseMapping(sourceMapping);
            List<List<Object>> targetRows = new ArrayList<>();

            for (List<Object> sourceRow : sourceData) {
                List<Object> targetRow = buildTargetRow(
                    sourceRow, sourceFieldToCol, targetMapping
                );
                targetRows.add(targetRow);
            }

            String range = "A" + targetDataStartRow;
            ValueRange body = new ValueRange().setValues(targetRows);
            googleSheetsService.spreadsheets().values()
                .update(newSpreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();

            log.info(
                "Data written to template. rows={}, targetStartRow={}",
                targetRows.size(), targetDataStartRow
            );

        } catch (IOException e) {
            log.error("Failed to write data to template. cause={}", e.getMessage(), e);
        }
    }

    private Map<String, Integer> buildReverseMapping(SheetColumnMapping mapping) {
        Map<String, Integer> result = new java.util.HashMap<>();
        for (String field : List.of(
            SheetColumnMapping.NAME, SheetColumnMapping.STUDENT_ID,
            SheetColumnMapping.EMAIL, SheetColumnMapping.PHONE,
            SheetColumnMapping.POSITION, SheetColumnMapping.JOINED_AT
        )) {
            int colIndex = mapping.getColumnIndex(field);
            if (colIndex >= 0) {
                result.put(field, colIndex);
            }
        }
        return result;
    }

    private List<Object> buildTargetRow(
        List<Object> sourceRow,
        Map<String, Integer> sourceFieldToCol,
        SheetColumnMapping targetMapping
    ) {
        int maxCol = targetMapping.toMap().values().stream()
            .filter(v -> v instanceof Integer)
            .mapToInt(v -> (Integer)v)
            .max()
            .orElse(0);

        List<Object> row = new ArrayList<>(
            Collections.nCopies(maxCol + 1, "")
        );

        for (Map.Entry<String, Integer> entry : sourceFieldToCol.entrySet()) {
            String field = entry.getKey();
            int sourceCol = entry.getValue();
            int targetCol = targetMapping.getColumnIndex(field);

            if (targetCol >= 0 && sourceCol < sourceRow.size()) {
                row.set(targetCol, sourceRow.get(sourceCol));
            }
        }

        return row;
    }
}
