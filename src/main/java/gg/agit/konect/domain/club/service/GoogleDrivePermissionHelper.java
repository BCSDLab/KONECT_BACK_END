package gg.agit.konect.domain.club.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;

final class GoogleDrivePermissionHelper {

    private static final int ROLE_RANK_NONE = 0;
    private static final int ROLE_RANK_READER = 1;
    private static final int ROLE_RANK_COMMENTER = 2;
    private static final int ROLE_RANK_WRITER = 3;
    private static final String PERMISSION_FIELDS =
        "nextPageToken,permissions(id,type,emailAddress,role)";
    private static final Set<String> SUPPORTED_TARGET_ROLES = Set.of(
        "reader",
        "commenter",
        "writer"
    );

    private GoogleDrivePermissionHelper() {}

    enum PermissionApplyStatus {
        CREATED,
        UPGRADED,
        UNCHANGED
    }

    static boolean hasRequiredRole(String currentRole, String targetRole) {
        return roleRank(currentRole) >= validateTargetRole(targetRole);
    }

    static List<Permission> listAllPermissions(Drive driveService, String fileId) throws IOException {
        List<Permission> permissions = new ArrayList<>();
        String nextPageToken = null;

        do {
            Drive.Permissions.List request = driveService.permissions().list(fileId)
                .setFields(PERMISSION_FIELDS);
            if (nextPageToken != null) {
                request.setPageToken(nextPageToken);
            }

            PermissionList response = request.execute();
            if (response.getPermissions() != null) {
                permissions.addAll(response.getPermissions());
            }
            nextPageToken = response.getNextPageToken();
        } while (nextPageToken != null && !nextPageToken.isBlank());

        return permissions;
    }

    private static int roleRank(String role) {
        if (role == null) {
            return ROLE_RANK_NONE;
        }

        return switch (role) {
            case "reader" -> ROLE_RANK_READER;
            case "commenter" -> ROLE_RANK_COMMENTER;
            case "writer", "fileOrganizer", "organizer", "owner" -> ROLE_RANK_WRITER;
            default -> ROLE_RANK_NONE;
        };
    }

    private static int validateTargetRole(String targetRole) {
        if (!SUPPORTED_TARGET_ROLES.contains(targetRole)) {
            throw new IllegalArgumentException("Unsupported targetRole: " + targetRole);
        }
        return roleRank(targetRole);
    }
}
