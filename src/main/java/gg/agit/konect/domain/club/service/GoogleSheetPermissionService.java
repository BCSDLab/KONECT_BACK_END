package gg.agit.konect.domain.club.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.repository.UserOAuthAccountRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.infrastructure.googlesheets.GoogleSheetsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleSheetPermissionService {

    private final GoogleCredentials googleCredentials;
    private final GoogleSheetsConfig googleSheetsConfig;
    private final UserOAuthAccountRepository userOAuthAccountRepository;

    public boolean tryGrantServiceAccountWriterAccess(Integer requesterId, String spreadsheetId) {
        String refreshToken = userOAuthAccountRepository
            .findByUserIdAndProvider(requesterId, Provider.GOOGLE)
            .map(account -> account.getGoogleDriveRefreshToken())
            .filter(StringUtils::hasText)
            .orElse(null);

        if (!StringUtils.hasText(refreshToken)) {
            log.warn(
                "Skipping service account auto-share because Google Drive OAuth is not connected. requesterId={}",
                requesterId
            );
            return false;
        }

        Drive userDriveService;
        try {
            userDriveService = googleSheetsConfig.buildUserDriveService(refreshToken);
        } catch (IOException | GeneralSecurityException e) {
            log.error("Failed to build user Drive service. requesterId={}", requesterId, e);
            throw CustomException.of(ApiResponseCode.FAILED_INIT_GOOGLE_DRIVE);
        }

        try {
            ensureServiceAccountPermission(userDriveService, spreadsheetId, "writer");
            return true;
        } catch (IOException e) {
            if (GoogleSheetApiExceptionHelper.isAccessDenied(e)
                || GoogleSheetApiExceptionHelper.isNotFound(e)) {
                log.warn(
                    "Failed to auto-share spreadsheet with service account. requesterId={}, spreadsheetId={}, cause={}",
                    requesterId,
                    spreadsheetId,
                    e.getMessage()
                );
                return false;
            }

            log.error(
                "Unexpected error while auto-sharing spreadsheet. requesterId={}, spreadsheetId={}",
                requesterId,
                spreadsheetId,
                e
            );
            throw CustomException.of(ApiResponseCode.FAILED_SYNC_GOOGLE_SHEET);
        }
    }

    private void ensureServiceAccountPermission(
        Drive userDriveService,
        String fileId,
        String targetRole
    ) throws IOException {
        String serviceAccountEmail = getServiceAccountEmail();
        Permission existingPermission = findServiceAccountPermission(
            userDriveService,
            fileId,
            serviceAccountEmail
        );

        if (existingPermission == null) {
            Permission permission = new Permission()
                .setType("user")
                .setRole(targetRole)
                .setEmailAddress(serviceAccountEmail);

            userDriveService.permissions().create(fileId, permission)
                .setSendNotificationEmail(false)
                .execute();
            log.info(
                "Service account access granted. fileId={}, role={}, email={}",
                fileId,
                targetRole,
                serviceAccountEmail
            );
            return;
        }

        String currentRole = existingPermission.getRole();
        if (roleRank(currentRole) >= roleRank(targetRole)) {
            log.info(
                "Service account permission already satisfies requested role. fileId={}, role={}, email={}",
                fileId,
                currentRole,
                serviceAccountEmail
            );
            return;
        }

        Permission updatedPermission = new Permission().setRole(targetRole);
        userDriveService.permissions().update(fileId, existingPermission.getId(), updatedPermission)
            .execute();
        log.info(
            "Service account permission upgraded. fileId={}, fromRole={}, toRole={}, email={}",
            fileId,
            currentRole,
            targetRole,
            serviceAccountEmail
        );
    }

    private Permission findServiceAccountPermission(
        Drive userDriveService,
        String fileId,
        String serviceAccountEmail
    ) throws IOException {
        List<Permission> permissions = userDriveService.permissions().list(fileId)
            .setFields("permissions(id,emailAddress,role)")
            .execute()
            .getPermissions();

        if (permissions == null) {
            return null;
        }

        return permissions.stream()
            .filter(permission -> serviceAccountEmail.equals(permission.getEmailAddress()))
            .findFirst()
            .orElse(null);
    }

    private String getServiceAccountEmail() {
        if (!(googleCredentials instanceof ServiceAccountCredentials serviceAccountCredentials)) {
            throw new IllegalStateException(
                "Google credentials is not a ServiceAccountCredentials. actual type="
                    + googleCredentials.getClass().getName()
            );
        }
        return serviceAccountCredentials.getClientEmail();
    }

    private int roleRank(String role) {
        if (role == null) {
            return 0;
        }

        return switch (role) {
            case "reader" -> 1;
            case "commenter" -> 2;
            case "writer", "fileOrganizer", "organizer", "owner" -> 3;
            default -> 0;
        };
    }
}
