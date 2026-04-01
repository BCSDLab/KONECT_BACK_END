package gg.agit.konect.domain.club.service;

import static gg.agit.konect.domain.club.service.GoogleApiTestUtils.googleException;
import static gg.agit.konect.domain.club.service.GoogleApiTestUtils.httpResponseException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import com.google.auth.oauth2.ServiceAccountCredentials;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.UserOAuthAccount;
import gg.agit.konect.domain.user.repository.UserOAuthAccountRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
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
    private Drive.Permissions.List nextPageListRequest;

    @Mock
    private Drive.Permissions.Create createRequest;

    @Mock
    private Drive.Permissions.Update updateRequest;

    @InjectMocks
    private GoogleSheetPermissionService googleSheetPermissionService;

    @Test
    @DisplayName("returns false when the requester has no Google Drive OAuth account")
    void tryGrantServiceAccountWriterAccessReturnsFalseWhenOAuthAccountIsMissing() {
        given(userOAuthAccountRepository.findByUserIdAndProvider(REQUESTER_ID, Provider.GOOGLE))
            .willReturn(Optional.empty());

        boolean granted = googleSheetPermissionService.tryGrantServiceAccountWriterAccess(
            REQUESTER_ID,
            FILE_ID
        );

        assertThat(granted).isFalse();
    }

    @Test
    @DisplayName("returns true without creating when the service account already has writer access")
    void tryGrantServiceAccountWriterAccessReturnsTrueWhenPermissionAlreadyExists()
        throws IOException, GeneralSecurityException {
        mockConnectedDriveAccount();
        given(permissions.list(FILE_ID)).willReturn(listRequest);
        given(listRequest.setFields("nextPageToken,permissions(id,type,emailAddress,role)"))
            .willReturn(listRequest);
        given(listRequest.execute()).willReturn(permissionList(permission("perm-1", "writer")));

        boolean granted = googleSheetPermissionService.tryGrantServiceAccountWriterAccess(
            REQUESTER_ID,
            FILE_ID
        );

        assertThat(granted).isTrue();
        verify(permissions, never()).create(eq(FILE_ID), any(Permission.class));
        verify(permissions, never()).update(eq(FILE_ID), eq("perm-1"), any(Permission.class));
    }

    @Test
    @DisplayName("finds existing permission across paged Drive permission results")
    void tryGrantServiceAccountWriterAccessFindsPermissionAcrossPages()
        throws IOException, GeneralSecurityException {
        mockConnectedDriveAccount();
        given(permissions.list(FILE_ID)).willReturn(listRequest, nextPageListRequest);
        given(listRequest.setFields("nextPageToken,permissions(id,type,emailAddress,role)"))
            .willReturn(listRequest);
        given(listRequest.execute()).willReturn(
            new PermissionList().setPermissions(List.of()).setNextPageToken("next-page")
        );
        given(nextPageListRequest.setFields("nextPageToken,permissions(id,type,emailAddress,role)"))
            .willReturn(nextPageListRequest);
        given(nextPageListRequest.setPageToken("next-page")).willReturn(nextPageListRequest);
        given(nextPageListRequest.execute()).willReturn(permissionList(permission("perm-1", "writer")));

        boolean granted = googleSheetPermissionService.tryGrantServiceAccountWriterAccess(
            REQUESTER_ID,
            FILE_ID
        );

        assertThat(granted).isTrue();
        verify(permissions, never()).create(eq(FILE_ID), any(Permission.class));
    }

    @Test
    @DisplayName("returns true when create fails but the permission is visible on re-check")
    void tryGrantServiceAccountWriterAccessReturnsTrueAfterConcurrentGrant()
        throws IOException, GeneralSecurityException {
        mockConnectedDriveAccount();
        given(permissions.list(FILE_ID)).willReturn(listRequest);
        given(listRequest.setFields("nextPageToken,permissions(id,type,emailAddress,role)"))
            .willReturn(listRequest);
        given(listRequest.execute()).willReturn(
            permissionList(),
            permissionList(permission("perm-1", "writer"))
        );
        given(permissions.create(eq(FILE_ID), any(Permission.class))).willReturn(createRequest);
        given(createRequest.setSendNotificationEmail(false)).willReturn(createRequest);
        given(createRequest.execute()).willThrow(new IOException("already granted"));

        boolean granted = googleSheetPermissionService.tryGrantServiceAccountWriterAccess(
            REQUESTER_ID,
            FILE_ID
        );

        assertThat(granted).isTrue();
        verify(permissions).create(eq(FILE_ID), any(Permission.class));
    }

    @Test
    @DisplayName("returns true when an existing permission needs to be upgraded to writer")
    void tryGrantServiceAccountWriterAccessUpgradesExistingPermission()
        throws IOException, GeneralSecurityException {
        mockConnectedDriveAccount();
        given(permissions.list(FILE_ID)).willReturn(listRequest);
        given(listRequest.setFields("nextPageToken,permissions(id,type,emailAddress,role)"))
            .willReturn(listRequest);
        given(listRequest.execute()).willReturn(permissionList(permission("perm-x", "reader")));
        given(permissions.update(eq(FILE_ID), eq("perm-x"), any(Permission.class))).willReturn(updateRequest);
        given(updateRequest.execute()).willReturn(permission("perm-x", "writer"));

        boolean granted = googleSheetPermissionService.tryGrantServiceAccountWriterAccess(
            REQUESTER_ID,
            FILE_ID
        );

        assertThat(granted).isTrue();
        verify(permissions).update(eq(FILE_ID), eq("perm-x"), any(Permission.class));
    }

    @Test
    @DisplayName("returns false when Google Drive auth fails during permission lookup")
    void tryGrantServiceAccountWriterAccessReturnsFalseWhenAuthFails()
        throws IOException, GeneralSecurityException {
        mockConnectedDriveAccount();
        given(permissions.list(FILE_ID)).willReturn(listRequest);
        given(listRequest.setFields("nextPageToken,permissions(id,type,emailAddress,role)"))
            .willReturn(listRequest);
        given(listRequest.execute()).willThrow(googleException(401, "authError"));

        boolean granted = googleSheetPermissionService.tryGrantServiceAccountWriterAccess(
            REQUESTER_ID,
            FILE_ID
        );

        assertThat(granted).isFalse();
    }

    @Test
    @DisplayName("returns false when Google Drive reports access denied while listing permissions")
    void tryGrantServiceAccountWriterAccessReturnsFalseWhenAccessIsDenied()
        throws IOException, GeneralSecurityException {
        mockConnectedDriveAccount();
        given(permissions.list(FILE_ID)).willReturn(listRequest);
        given(listRequest.setFields("nextPageToken,permissions(id,type,emailAddress,role)"))
            .willReturn(listRequest);
        given(listRequest.execute()).willThrow(googleException(403, "forbidden"));

        boolean granted = googleSheetPermissionService.tryGrantServiceAccountWriterAccess(
            REQUESTER_ID,
            FILE_ID
        );

        assertThat(granted).isFalse();
    }

    @Test
    @DisplayName("throws a bad request custom exception when Google returns invalid_grant")
    void tryGrantServiceAccountWriterAccessThrowsWhenInvalidGrantOccurs()
        throws IOException, GeneralSecurityException {
        mockConnectedDriveAccount();
        given(permissions.list(FILE_ID)).willReturn(listRequest);
        given(listRequest.setFields("nextPageToken,permissions(id,type,emailAddress,role)"))
            .willReturn(listRequest);
        given(listRequest.execute()).willThrow(new IOException(
            "token refresh failed",
            httpResponseException(
                400,
                "{\"error\":\"invalid_grant\",\"error_description\":\"Bad Request\"}"
            )
        ));

        assertThatThrownBy(() -> googleSheetPermissionService.tryGrantServiceAccountWriterAccess(
            REQUESTER_ID,
            FILE_ID
        ))
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(ApiResponseCode.INVALID_GOOGLE_DRIVE_AUTH);
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
}
