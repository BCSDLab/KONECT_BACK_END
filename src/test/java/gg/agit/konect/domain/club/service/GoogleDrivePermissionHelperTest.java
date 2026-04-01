package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;

import gg.agit.konect.support.ServiceTestSupport;

class GoogleDrivePermissionHelperTest extends ServiceTestSupport {

    private static final String FILE_ID = "spreadsheet-id";

    @Mock
    private Drive driveService;

    @Mock
    private Drive.Permissions permissions;

    @Mock
    private Drive.Permissions.List firstListRequest;

    @Mock
    private Drive.Permissions.List secondListRequest;

    @Mock
    private Drive.Permissions.Create createRequest;

    @Mock
    private Drive.Permissions.Update updateRequest;

    @Test
    @DisplayName("returns true only when the current role satisfies the target role")
    void hasRequiredRoleReturnsExpectedResult() {
        assertThat(GoogleDrivePermissionHelper.hasRequiredRole("reader", "reader")).isTrue();
        assertThat(GoogleDrivePermissionHelper.hasRequiredRole("commenter", "reader")).isTrue();
        assertThat(GoogleDrivePermissionHelper.hasRequiredRole("writer", "commenter")).isTrue();
        assertThat(GoogleDrivePermissionHelper.hasRequiredRole("reader", "commenter")).isFalse();
        assertThat(GoogleDrivePermissionHelper.hasRequiredRole("commenter", "writer")).isFalse();
    }

    @Test
    @DisplayName("throws when target role is unsupported")
    void hasRequiredRoleThrowsWhenTargetRoleIsUnsupported() {
        assertThatThrownBy(() -> GoogleDrivePermissionHelper.hasRequiredRole("reader", "invalid-role"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported targetRole");
    }

    @Test
    @DisplayName("lists all permissions across paged Drive responses")
    void listAllPermissionsReturnsPermissionsAcrossPages() throws IOException {
        Permission firstPermission = new Permission().setId("perm-1");
        Permission secondPermission = new Permission().setId("perm-2");

        given(driveService.permissions()).willReturn(permissions);
        given(permissions.list(FILE_ID)).willReturn(firstListRequest, secondListRequest);
        given(firstListRequest.setFields("nextPageToken,permissions(id,type,emailAddress,role)"))
            .willReturn(firstListRequest);
        given(firstListRequest.execute()).willReturn(
            new PermissionList()
                .setPermissions(List.of(firstPermission))
                .setNextPageToken("next-page")
        );
        given(secondListRequest.setFields("nextPageToken,permissions(id,type,emailAddress,role)"))
            .willReturn(secondListRequest);
        given(secondListRequest.setPageToken("next-page")).willReturn(secondListRequest);
        given(secondListRequest.execute()).willReturn(
            new PermissionList().setPermissions(List.of(secondPermission))
        );

        assertThat(GoogleDrivePermissionHelper.listAllPermissions(driveService, FILE_ID))
            .containsExactly(firstPermission, secondPermission);
    }

    @Test
    @DisplayName("returns created when create succeeds after a retry check")
    void ensureServiceAccountPermissionReturnsCreatedWhenPermissionAppearsAfterRetry() throws IOException {
        Drive.Permissions.List initialListRequest = mock(Drive.Permissions.List.class);
        Drive.Permissions.List applyListRequest = mock(Drive.Permissions.List.class);
        Drive.Permissions.List recheckListRequest = mock(Drive.Permissions.List.class);

        given(driveService.permissions()).willReturn(permissions);
        given(permissions.list(FILE_ID)).willReturn(initialListRequest, applyListRequest, recheckListRequest);
        given(initialListRequest.setFields("nextPageToken,permissions(id,type,emailAddress,role)"))
            .willReturn(initialListRequest);
        given(applyListRequest.setFields("nextPageToken,permissions(id,type,emailAddress,role)"))
            .willReturn(applyListRequest);
        given(recheckListRequest.setFields("nextPageToken,permissions(id,type,emailAddress,role)"))
            .willReturn(recheckListRequest);
        given(initialListRequest.execute()).willReturn(new PermissionList().setPermissions(List.of()));
        given(applyListRequest.execute()).willReturn(new PermissionList().setPermissions(List.of()));
        given(recheckListRequest.execute()).willReturn(
            new PermissionList().setPermissions(List.of(serviceAccountPermission("perm-1", "writer")))
        );
        given(permissions.create(eq(FILE_ID), org.mockito.ArgumentMatchers.any(Permission.class)))
            .willReturn(createRequest);
        given(createRequest.setSendNotificationEmail(false)).willReturn(createRequest);
        given(createRequest.execute()).willThrow(new IOException("create failed after applying"));

        assertThat(
            GoogleDrivePermissionHelper.ensureServiceAccountPermission(
                driveService,
                FILE_ID,
                "writer",
                "service-account@project.iam.gserviceaccount.com"
            )
        ).isEqualTo(GoogleDrivePermissionHelper.PermissionApplyStatus.CREATED);
    }

    @Test
    @DisplayName("returns upgraded when update succeeds after a retry check")
    void ensureServiceAccountPermissionReturnsUpgradedWhenPermissionImprovesAfterRetry() throws IOException {
        Drive.Permissions.List initialListRequest = mock(Drive.Permissions.List.class);
        Drive.Permissions.List applyListRequest = mock(Drive.Permissions.List.class);
        Drive.Permissions.List recheckListRequest = mock(Drive.Permissions.List.class);

        given(driveService.permissions()).willReturn(permissions);
        given(permissions.list(FILE_ID)).willReturn(initialListRequest, applyListRequest, recheckListRequest);
        given(initialListRequest.setFields("nextPageToken,permissions(id,type,emailAddress,role)"))
            .willReturn(initialListRequest);
        given(applyListRequest.setFields("nextPageToken,permissions(id,type,emailAddress,role)"))
            .willReturn(applyListRequest);
        given(recheckListRequest.setFields("nextPageToken,permissions(id,type,emailAddress,role)"))
            .willReturn(recheckListRequest);
        given(initialListRequest.execute()).willReturn(
            new PermissionList().setPermissions(List.of(serviceAccountPermission("perm-1", "reader")))
        );
        given(applyListRequest.execute()).willReturn(
            new PermissionList().setPermissions(List.of(serviceAccountPermission("perm-1", "reader")))
        );
        given(recheckListRequest.execute()).willReturn(
            new PermissionList().setPermissions(List.of(serviceAccountPermission("perm-1", "writer")))
        );
        given(permissions.update(eq(FILE_ID), eq("perm-1"), org.mockito.ArgumentMatchers.any(Permission.class)))
            .willReturn(updateRequest);
        given(updateRequest.execute()).willThrow(new IOException("update failed after applying"));

        assertThat(
            GoogleDrivePermissionHelper.ensureServiceAccountPermission(
                driveService,
                FILE_ID,
                "writer",
                "service-account@project.iam.gserviceaccount.com"
            )
        ).isEqualTo(GoogleDrivePermissionHelper.PermissionApplyStatus.UPGRADED);
    }

    @Test
    @DisplayName("throws when ensure is called with unsupported target role")
    void ensureServiceAccountPermissionThrowsWhenTargetRoleIsUnsupported() {
        assertThatThrownBy(
            () -> GoogleDrivePermissionHelper.ensureServiceAccountPermission(
                driveService,
                FILE_ID,
                "invalid-role",
                "service-account@project.iam.gserviceaccount.com"
            )
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported targetRole");
    }

    private Permission serviceAccountPermission(String permissionId, String role) {
        return new Permission()
            .setId(permissionId)
            .setType("user")
            .setEmailAddress("service-account@project.iam.gserviceaccount.com")
            .setRole(role);
    }
}
