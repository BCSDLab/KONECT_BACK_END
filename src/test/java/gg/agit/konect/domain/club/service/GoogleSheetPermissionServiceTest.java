package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import com.google.auth.oauth2.ServiceAccountCredentials;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.UserOAuthAccount;
import gg.agit.konect.domain.user.repository.UserOAuthAccountRepository;
import gg.agit.konect.infrastructure.googlesheets.GoogleSheetsConfig;
import gg.agit.konect.support.ServiceTestSupport;

class GoogleSheetPermissionServiceTest extends ServiceTestSupport {

    private static final Integer REQUESTER_ID = 1;
    private static final String FILE_ID = "spreadsheet-id";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String SERVICE_ACCOUNT_EMAIL = "service-account@konect.iam.gserviceaccount.com";

    @Mock
    private ServiceAccountCredentials serviceAccountCredentials;

    @Mock
    private GoogleSheetsConfig googleSheetsConfig;

    @Mock
    private UserOAuthAccountRepository userOAuthAccountRepository;

    @Mock
    private UserOAuthAccount userOAuthAccount;

    @Mock
    private Drive userDriveService;

    @Mock
    private Drive.Permissions permissions;

    @Mock
    private Drive.Permissions.List listRequest;

    @Mock
    private Drive.Permissions.Create createRequest;

    @Mock
    private Drive.Permissions.Update updateRequest;

    @InjectMocks
    private GoogleSheetPermissionService googleSheetPermissionService;

    @Test
    @DisplayName("returns true without creating when the service account already has writer access")
    void tryGrantServiceAccountWriterAccessReturnsTrueWhenPermissionAlreadyExists()
        throws IOException, GeneralSecurityException {
        // given
        mockConnectedDriveAccount();
        given(permissions.list(FILE_ID)).willReturn(listRequest);
        given(listRequest.setFields("permissions(id,emailAddress,role)")).willReturn(listRequest);
        given(listRequest.execute()).willReturn(permissionList(permission("perm-1", "writer")));

        // when
        boolean granted = googleSheetPermissionService.tryGrantServiceAccountWriterAccess(
            REQUESTER_ID,
            FILE_ID
        );

        // then
        assertThat(granted).isTrue();
        verify(permissions, never()).create(eq(FILE_ID), any(Permission.class));
        verify(permissions, never()).update(eq(FILE_ID), eq("perm-1"), any(Permission.class));
    }

    @Test
    @DisplayName("returns true when create fails but the permission is visible on re-check")
    void tryGrantServiceAccountWriterAccessReturnsTrueAfterConcurrentGrant()
        throws IOException, GeneralSecurityException {
        // given
        mockConnectedDriveAccount();
        given(permissions.list(FILE_ID)).willReturn(listRequest);
        given(listRequest.setFields("permissions(id,emailAddress,role)")).willReturn(listRequest);
        given(listRequest.execute()).willReturn(
            permissionList(),
            permissionList(permission("perm-1", "writer"))
        );
        given(permissions.create(eq(FILE_ID), any(Permission.class))).willReturn(createRequest);
        given(createRequest.setSendNotificationEmail(false)).willReturn(createRequest);
        given(createRequest.execute()).willThrow(new IOException("already granted"));

        // when
        boolean granted = googleSheetPermissionService.tryGrantServiceAccountWriterAccess(
            REQUESTER_ID,
            FILE_ID
        );

        // then
        assertThat(granted).isTrue();
        verify(permissions).create(eq(FILE_ID), any(Permission.class));
    }

    @Test
    @DisplayName("returns false when Google Drive auth fails during permission lookup")
    void tryGrantServiceAccountWriterAccessReturnsFalseWhenAuthFails()
        throws IOException, GeneralSecurityException {
        // given
        mockConnectedDriveAccount();
        given(permissions.list(FILE_ID)).willReturn(listRequest);
        given(listRequest.setFields("permissions(id,emailAddress,role)")).willReturn(listRequest);
        given(listRequest.execute()).willThrow(googleException(401, "authError"));

        // when
        boolean granted = googleSheetPermissionService.tryGrantServiceAccountWriterAccess(
            REQUESTER_ID,
            FILE_ID
        );

        // then
        assertThat(granted).isFalse();
    }

    private void mockConnectedDriveAccount() throws IOException, GeneralSecurityException {
        given(userOAuthAccountRepository.findByUserIdAndProvider(REQUESTER_ID, Provider.GOOGLE))
            .willReturn(Optional.of(userOAuthAccount));
        given(userOAuthAccount.getGoogleDriveRefreshToken()).willReturn(REFRESH_TOKEN);
        given(googleSheetsConfig.buildUserDriveService(REFRESH_TOKEN)).willReturn(userDriveService);
        given(serviceAccountCredentials.getClientEmail()).willReturn(SERVICE_ACCOUNT_EMAIL);
        given(userDriveService.permissions()).willReturn(permissions);
    }

    private Permission permission(String id, String role) {
        return new Permission()
            .setId(id)
            .setType("user")
            .setEmailAddress(SERVICE_ACCOUNT_EMAIL)
            .setRole(role);
    }

    private PermissionList permissionList(Permission... permissions) {
        return new PermissionList().setPermissions(List.of(permissions));
    }

    private GoogleJsonResponseException googleException(int statusCode, String reason) {
        GoogleJsonError.ErrorInfo errorInfo = new GoogleJsonError.ErrorInfo();
        errorInfo.setReason(reason);

        GoogleJsonError error = new GoogleJsonError();
        error.setCode(statusCode);
        error.setErrors(List.of(errorInfo));

        HttpResponseException.Builder builder = new HttpResponseException.Builder(
            statusCode,
            null,
            new HttpHeaders()
        );

        return new GoogleJsonResponseException(builder, error);
    }
}
