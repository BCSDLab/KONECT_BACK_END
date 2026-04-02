package gg.agit.konect.domain.club.service;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
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
    private final Drive googleDriveService;
    private final GoogleSheetsConfig googleSheetsConfig;
    private final UserOAuthAccountRepository userOAuthAccountRepository;

    public void validateRequesterAccessAndTryGrantServiceAccountWriterAccess(
        Integer requesterId,
        String spreadsheetId
    ) {
        String refreshToken = requireRefreshToken(requesterId);
        Drive userDriveService = buildUserDriveService(refreshToken, requesterId);
        validateRequesterSpreadsheetAccess(userDriveService, requesterId, spreadsheetId);
        boolean granted = tryGrantServiceAccountWriterAccess(userDriveService, requesterId, spreadsheetId);
        if (!granted) {
            requireServiceAccountSpreadsheetAccess(spreadsheetId, requesterId);
        }
    }

    public boolean tryGrantServiceAccountWriterAccess(Integer requesterId, String spreadsheetId) {
        String refreshToken = resolveRefreshToken(requesterId);

        if (refreshToken == null) {
            log.warn(
                "Skipping service account auto-share because Google Drive OAuth is not connected. requesterId={}",
                requesterId
            );
            return false;
        }

        Drive userDriveService = buildUserDriveService(refreshToken, requesterId);
        return tryGrantServiceAccountWriterAccess(userDriveService, requesterId, spreadsheetId);
    }

    private String requireRefreshToken(Integer requesterId) {
        return userOAuthAccountRepository.findByUserIdAndProvider(requesterId, Provider.GOOGLE)
            .map(account -> account.getGoogleDriveRefreshToken())
            .filter(StringUtils::hasText)
            .orElseThrow(() -> {
                log.warn(
                    "Rejecting spreadsheet registration because Google Drive OAuth is not connected. requesterId={}",
                    requesterId
                );
                return CustomException.of(ApiResponseCode.NOT_FOUND_GOOGLE_DRIVE_AUTH);
            });
    }

    private String resolveRefreshToken(Integer requesterId) {
        return userOAuthAccountRepository.findByUserIdAndProvider(requesterId, Provider.GOOGLE)
            .map(account -> account.getGoogleDriveRefreshToken())
            .filter(StringUtils::hasText)
            .orElse(null);
    }

    private boolean tryGrantServiceAccountWriterAccess(
        Drive userDriveService,
        Integer requesterId,
        String spreadsheetId
    ) {
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

    private Drive buildUserDriveService(String refreshToken, Integer requesterId) {
        try {
            return googleSheetsConfig.buildUserDriveService(refreshToken);
        } catch (IOException | GeneralSecurityException e) {
            log.error("Failed to build user Drive service. requesterId={}", requesterId, e);
            throw CustomException.of(ApiResponseCode.FAILED_INIT_GOOGLE_DRIVE);
        }
    }

    private void validateRequesterSpreadsheetAccess(
        Drive userDriveService,
        Integer requesterId,
        String spreadsheetId
    ) {
        try {
            File file = userDriveService.files().get(spreadsheetId)
                .setFields("id")
                .setSupportsAllDrives(true)
                .execute();
            if (file == null || !StringUtils.hasText(file.getId())) {
                throw GoogleSheetApiExceptionHelper.accessDenied();
            }
        } catch (IOException e) {
            if (GoogleSheetApiExceptionHelper.isInvalidGrant(e)) {
                log.warn(
                    "Google Drive OAuth token is invalid while validating spreadsheet access. requesterId={}, "
                        + "spreadsheetId={}, cause={}",
                    requesterId,
                    spreadsheetId,
                    GoogleSheetApiExceptionHelper.extractDetail(e)
                );
                throw GoogleSheetApiExceptionHelper.invalidGoogleDriveAuth(e);
            }

            if (GoogleSheetApiExceptionHelper.isAuthFailure(e)) {
                log.warn(
                    "Google Drive OAuth auth failure while validating spreadsheet access. requesterId={}, "
                        + "spreadsheetId={}, cause={}",
                    requesterId,
                    spreadsheetId,
                    GoogleSheetApiExceptionHelper.extractDetail(e)
                );
                throw GoogleSheetApiExceptionHelper.invalidGoogleDriveAuth(e);
            }

            if (GoogleSheetApiExceptionHelper.isAccessDenied(e)
                || GoogleSheetApiExceptionHelper.isNotFound(e)) {
                log.warn(
                    "Requester has no spreadsheet access. requesterId={}, spreadsheetId={}, cause={}",
                    requesterId,
                    spreadsheetId,
                    e.getMessage()
                );
                throw GoogleSheetApiExceptionHelper.accessDenied();
            }

            log.error(
                "Unexpected error while validating requester spreadsheet access. requesterId={}, spreadsheetId={}",
                requesterId,
                spreadsheetId,
                e
            );
            throw CustomException.of(ApiResponseCode.FAILED_SYNC_GOOGLE_SHEET);
        }
    }

    private void requireServiceAccountSpreadsheetAccess(String spreadsheetId, Integer requesterId) {
        try {
            File file = googleDriveService.files().get(spreadsheetId)
                .setFields("id")
                .setSupportsAllDrives(true)
                .execute();
            if (file == null || !StringUtils.hasText(file.getId())) {
                throw GoogleSheetApiExceptionHelper.accessDenied();
            }
        } catch (IOException e) {
            if (GoogleSheetApiExceptionHelper.isAccessDenied(e)
                || GoogleSheetApiExceptionHelper.isNotFound(e)) {
                log.warn(
                    "Service account has no spreadsheet access after auto-share failed. requesterId={}, "
                        + "spreadsheetId={}, cause={}",
                    requesterId,
                    spreadsheetId,
                    e.getMessage()
                );
                throw GoogleSheetApiExceptionHelper.accessDenied();
            }

            log.error(
                "Unexpected error while re-checking service account spreadsheet access. requesterId={}, "
                    + "spreadsheetId={}",
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
