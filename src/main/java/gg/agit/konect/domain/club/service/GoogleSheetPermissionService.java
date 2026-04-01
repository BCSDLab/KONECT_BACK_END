package gg.agit.konect.domain.club.service;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.api.services.drive.Drive;
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

    private final ServiceAccountCredentials serviceAccountCredentials;
    private final GoogleSheetsConfig googleSheetsConfig;
    private final UserOAuthAccountRepository userOAuthAccountRepository;

    public boolean tryGrantServiceAccountWriterAccess(Integer requesterId, String spreadsheetId) {
        String refreshToken = userOAuthAccountRepository
            .findByUserIdAndProvider(requesterId, Provider.GOOGLE)
            .map(account -> account.getGoogleDriveRefreshToken())
            .filter(StringUtils::hasText)
            .orElse(null);

        if (refreshToken == null) {
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
            GoogleDrivePermissionHelper.ensureServiceAccountPermission(
                userDriveService,
                spreadsheetId,
                "writer",
                getServiceAccountEmail()
            );
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

    private String getServiceAccountEmail() {
        return serviceAccountCredentials.getClientEmail();
    }
}
