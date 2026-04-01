package gg.agit.konect.domain.club.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
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

    private static final int PERMISSION_APPLY_MAX_ATTEMPTS = 2;

    private final ServiceAccountCredentials serviceAccountCredentials;
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
            if (GoogleSheetApiExceptionHelper.isInvalidGrant(e)) {
                log.warn(
                    "Google Drive OAuth token is invalid while auto-sharing spreadsheet. requesterId={}, "
                        + "spreadsheetId={}, cause={}",
                    requesterId,
                    spreadsheetId,
                    GoogleSheetApiExceptionHelper.extractDetail(e)
                );
                throw GoogleSheetApiExceptionHelper.invalidGoogleDriveAuth(e);
            }

            if (GoogleSheetApiExceptionHelper.isAccessDenied(e)
                || GoogleSheetApiExceptionHelper.isAuthFailure(e)
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

        for (int attempt = 1; attempt <= PERMISSION_APPLY_MAX_ATTEMPTS; attempt++) {
            try {
                applyServiceAccountPermission(
                    userDriveService,
                    fileId,
                    targetRole,
                    serviceAccountEmail
                );
                return;
            } catch (IOException e) {
                if (hasRequiredPermission(
                    userDriveService,
                    fileId,
                    serviceAccountEmail,
                    targetRole
                )) {
                    log.info(
                        "Service account permission reached target role after retry. fileId={}, role={}, email={}",
                        fileId,
                        targetRole,
                        serviceAccountEmail
                    );
                    return;
                }

                if (attempt == PERMISSION_APPLY_MAX_ATTEMPTS) {
                    throw e;
                }
            }
        }
    }

    private void applyServiceAccountPermission(
        Drive userDriveService,
        String fileId,
        String targetRole,
        String serviceAccountEmail
    ) throws IOException {
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
        if (GoogleDrivePermissionHelper.hasRequiredRole(currentRole, targetRole)) {
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

    private boolean hasRequiredPermission(
        Drive userDriveService,
        String fileId,
        String serviceAccountEmail,
        String targetRole
    ) {
        try {
            Permission currentPermission = findServiceAccountPermission(
                userDriveService,
                fileId,
                serviceAccountEmail
            );
            return currentPermission != null
                && GoogleDrivePermissionHelper.hasRequiredRole(currentPermission.getRole(), targetRole);
        } catch (IOException e) {
            log.debug(
                "Failed to re-check service account permission. fileId={}, email={}, cause={}",
                fileId,
                serviceAccountEmail,
                e.getMessage()
            );
            return false;
        }
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
        return serviceAccountCredentials.getClientEmail();
    }
}
