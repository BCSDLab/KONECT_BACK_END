package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

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
}
