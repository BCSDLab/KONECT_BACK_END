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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.SheetColumnMapping;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.UserOAuthAccount;
import gg.agit.konect.domain.user.repository.UserOAuthAccountRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.global.util.PhoneNumberNormalizer;
import gg.agit.konect.infrastructure.googlesheets.GoogleSheetsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SheetMigrationService {

    private static final Pattern FOLDER_ID_PATTERN =
        Pattern.compile("(?:folders/|id=)([a-zA-Z0-9_-]{20,})");
    private static final String MIME_TYPE_SPREADSHEET =
        "application/vnd.google-apps.spreadsheet";
    private static final String NEW_SHEET_TITLE_PREFIX = "KONECT_인명부_";

    @Value("${google.sheets.template-spreadsheet-id:}")
    private String defaultTemplateSpreadsheetId;

    private final Sheets googleSheetsService;
    private final GoogleCredentials googleCredentials;
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
            throw CustomException.of(ApiResponseCode.FAILED_INIT_GOOGLE_DRIVE);
        }

        String sourceSpreadsheetId = SpreadsheetUrlParser.extractId(sourceSpreadsheetUrl);
        String folderId = resolveFolderId(userDriveService, sourceSpreadsheetUrl, sourceSpreadsheetId);

        // 소스 파일에 서비스 계정 reader 권한을 먼저 부여해야 readAllData()가 성공함
        grantServiceAccountReadAccess(userDriveService, sourceSpreadsheetId);
        // 트랜잭션 실패 / 완료 후 소스 파일 서비스 계정 권한 제거 (보상 처리)
        registerSourceFilePermissionCleanup(userDriveService, sourceSpreadsheetId);

        String newSpreadsheetId = copyTemplate(userDriveService, templateId, club.getName(), folderId);
        registerDriveRollback(userDriveService, newSpreadsheetId);
        grantServiceAccountAccess(userDriveService, newSpreadsheetId);

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

    /**
     * 소스 파일에 서비스 계정 reader 권한을 부여합니다.
     * migrate 시 서비스 계정 Sheets API로 소스 데이터를 읽어야 하므로 필요합니다.
     */
    private void grantServiceAccountReadAccess(Drive userDriveService, String fileId) {
        grantServiceAccountPermission(userDriveService, fileId, "reader");
    }

    /**
     * 트랜잭션 완료(성공/실패 모두) 후 소스 파일에서 서비스 계정 권한을 제거합니다.
     * 서비스 계정의 파일 접근을 최소화하기 위한 보상 처리입니다.
     */
    private void registerSourceFilePermissionCleanup(Drive driveService, String fileId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                removeServiceAccountPermission(driveService, fileId);
            }
        });
    }

    private void removeServiceAccountPermission(Drive driveService, String fileId) {
        if (!(googleCredentials instanceof ServiceAccountCredentials sac)) {
            return;
        }
        String serviceAccountEmail = sac.getClientEmail();
        try {
            // permissionId 조회 후 삭제 (getPermissions()는 빈 경우 null 반환 가능)
            List<Permission> permissions =
                driveService.permissions().list(fileId)
                    .setFields("permissions(id,emailAddress)")
                    .execute()
                    .getPermissions();
            if (permissions == null) {
                return;
            }
            permissions.stream()
                .filter(p -> serviceAccountEmail.equals(p.getEmailAddress()))
                .findFirst()
                .ifPresent(p -> {
                    try {
                        driveService.permissions().delete(fileId, p.getId()).execute();
                        log.info(
                            "Service account permission removed from source file. fileId={}",
                            fileId
                        );
                    } catch (IOException ex) {
                        log.warn(
                            "Failed to remove service account permission. fileId={}, cause={}",
                            fileId, ex.getMessage()
                        );
                    }
                });
        } catch (IOException e) {
            log.warn(
                "Failed to list permissions for source file cleanup. fileId={}, cause={}",
                fileId, e.getMessage()
            );
        }
    }

    private void grantServiceAccountAccess(Drive userDriveService, String fileId) {
        grantServiceAccountPermission(userDriveService, fileId, "writer");
    }

    /**
     * 서비스 계정에 지정된 role로 Drive 접근 권한을 부여하는 공통 메서드입니다.
     */
    private void grantServiceAccountPermission(Drive userDriveService, String fileId, String role) {
        if (!(googleCredentials instanceof ServiceAccountCredentials sac)) {
            throw new IllegalStateException(
                "Google credentials is not a ServiceAccountCredentials. actual type="
                    + googleCredentials.getClass().getName()
            );
        }
        String serviceAccountEmail = sac.getClientEmail();
        try {
            Permission permission = new Permission()
                .setType("user")
                .setRole(role)
                .setEmailAddress(serviceAccountEmail);
            userDriveService.permissions().create(fileId, permission)
                .setSendNotificationEmail(false)
                .execute();
            log.info(
                "Service account {} access granted. fileId={}, email={}",
                role, fileId, serviceAccountEmail
            );
        } catch (IOException e) {
            if (GoogleSheetApiExceptionHelper.isAccessDenied(e)) {
                log.warn(
                    "Google Sheets access denied while granting service account permission. "
                        + "fileId={}, role={}, cause={}",
                    fileId,
                    role,
                    e.getMessage()
                );
                throw GoogleSheetApiExceptionHelper.accessDenied(
                    "fileId=" + fileId + ", role=" + role
                );
            }
            log.error(
                "Failed to grant service account {} access. fileId={}, cause={}",
                role, fileId, e.getMessage(), e
            );
            throw CustomException.of(ApiResponseCode.FAILED_SYNC_GOOGLE_SHEET);
        }
    }

    private void registerDriveRollback(Drive driveService, String fileId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    deleteFile(driveService, fileId);
                }
            }
        });
    }

    private void deleteFile(Drive driveService, String fileId) {
        try {
            driveService.files().delete(fileId).execute();
            log.info("Orphaned file deleted. fileId={}", fileId);
        } catch (IOException ex) {
            log.warn("Failed to delete orphaned file. fileId={}, cause={}", fileId, ex.getMessage());
        }
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
        // newFileId를 try 바깥에서 선언하여 예외 경로에서도 고아 파일 정리가 가능하도록 함
        String newFileId = null;
        try {
            String title = NEW_SHEET_TITLE_PREFIX + clubName;
            File copyMetadata = new File().setName(title);

            // Drive API v3에서 files().copy() 바디의 parents 필드는 무시됨.
            // 복사 후 files().update()로 addParents/removeParents를 명시적으로 호출해야 폴더 이동이 적용됨.
            File copied = driveService.files().copy(templateId, copyMetadata)
                .setFields("id, parents")
                .execute();

            newFileId = copied.getId();

            if (targetFolderId != null) {
                List<String> currentParents = copied.getParents();
                // copy() 응답에서 parents가 null로 오는 경우 별도 GET 으로 재조회
                if (currentParents == null || currentParents.isEmpty()) {
                    try {
                        File fileInfo = driveService.files().get(newFileId)
                            .setFields("parents")
                            .execute();
                        currentParents = fileInfo.getParents();
                        log.debug(
                            "Re-fetched parents for copied file. fileId={}, parents={}",
                            newFileId, currentParents
                        );
                    } catch (IOException ex) {
                        log.error(
                            "Failed to re-fetch parents for copied file. fileId={}, cause={}",
                            newFileId, ex.getMessage()
                        );
                        deleteFile(driveService, newFileId);
                        throw CustomException.of(ApiResponseCode.FAILED_SYNC_GOOGLE_SHEET);
                    }
                }
                // parents를 끝내 확보하지 못한 경우, 폴더 이동이 보장되지 않으므로 예외로 처리해 롤백
                if (currentParents == null || currentParents.isEmpty()) {
                    log.error("Cannot determine parents for copied file. fileId={}", newFileId);
                    deleteFile(driveService, newFileId);
                    throw CustomException.of(ApiResponseCode.FAILED_SYNC_GOOGLE_SHEET);
                }
                String removeParents = String.join(",", currentParents);
                driveService.files().update(newFileId, new File())
                    .setAddParents(targetFolderId)
                    .setRemoveParents(removeParents)
                    .setFields("id, parents")
                    .execute();
            }

            log.info("Template copied by user. newId={}, folderId={}", newFileId, targetFolderId);
            return newFileId;

        } catch (IOException e) {
            if (newFileId != null) {
                deleteFile(driveService, newFileId);
            }
            if (GoogleSheetApiExceptionHelper.isAccessDenied(e)) {
                log.warn(
                    "Google Sheets access denied while copying template. cause={}",
                    e.getMessage()
                );
                throw GoogleSheetApiExceptionHelper.accessDenied(
                    "templateId=" + templateId + ", targetFolderId=" + targetFolderId
                );
            }
            log.error("Failed to copy template. cause={}", e.getMessage(), e);
            throw CustomException.of(ApiResponseCode.FAILED_SYNC_GOOGLE_SHEET);
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
                .setValueRenderOption("FORMATTED_VALUE")
                .execute();

            List<List<Object>> values = response.getValues();
            return values != null ? values : List.of();

        } catch (IOException e) {
            if (GoogleSheetApiExceptionHelper.isAccessDenied(e)) {
                log.warn(
                    "Google Sheets access denied while reading source data. spreadsheetId={}, cause={}",
                    spreadsheetId,
                    e.getMessage()
                );
                throw GoogleSheetApiExceptionHelper.accessDenied(
                    "spreadsheetId=" + spreadsheetId
                );
            }
            log.error("Failed to read source data. cause={}", e.getMessage(), e);
            throw CustomException.of(ApiResponseCode.FAILED_SYNC_GOOGLE_SHEET);
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
            if (GoogleSheetApiExceptionHelper.isAccessDenied(e)) {
                log.warn(
                    "Google Sheets access denied while writing template data. spreadsheetId={}, cause={}",
                    newSpreadsheetId,
                    e.getMessage()
                );
                throw GoogleSheetApiExceptionHelper.accessDenied(
                    "spreadsheetId=" + newSpreadsheetId
                );
            }
            log.error("Failed to write data to template. cause={}", e.getMessage(), e);
            throw CustomException.of(ApiResponseCode.FAILED_SYNC_GOOGLE_SHEET);
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
                Object cellValue = sourceRow.get(sourceCol);
                // 전화번호 컬럼은 010-xxxx-xxxx 형식으로 포맷팅 (0 잘림 복구 포함)
                if (SheetColumnMapping.PHONE.equals(field) && cellValue != null) {
                    cellValue = PhoneNumberNormalizer.format(cellValue.toString());
                }
                row.set(targetCol, cellValue);
            }
        }

        return row;
    }
}
