package gg.agit.konect.domain.club.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class GoogleDrivePermissionHelper {

    private static final int PERMISSION_APPLY_MAX_ATTEMPTS = 2;
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

    static PermissionApplyStatus ensureServiceAccountPermission(
        Drive userDriveService,
        String fileId,
        String targetRole,
        String serviceAccountEmail
    ) throws IOException {
        validateTargetRole(targetRole);
        Permission initialPermission = findServiceAccountPermission(
            userDriveService,
            fileId,
            serviceAccountEmail
        );
        int attempt = 1;
        while (true) {
            try {
                return applyServiceAccountPermission(
                    userDriveService,
                    fileId,
                    targetRole,
                    serviceAccountEmail
                );
            } catch (IOException e) {
                PermissionApplyStatus recoveredStatus = recoverPermissionApplyStatus(
                    userDriveService,
                    fileId,
                    serviceAccountEmail,
                    targetRole,
                    initialPermission,
                    attempt
                );
                if (recoveredStatus != null) {
                    log.info(
                        "Service account permission reached target role after attempt {}. "
                            + "fileId={}, role={}, email={}, status={}",
                        attempt,
                        fileId,
                        targetRole,
                        serviceAccountEmail,
                        recoveredStatus
                    );
                    return recoveredStatus;
                }

                if (attempt++ >= PERMISSION_APPLY_MAX_ATTEMPTS) {
                    throw e;
                }
            }
        }
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

    static Permission findServiceAccountPermission(
        Drive userDriveService,
        String fileId,
        String serviceAccountEmail
    ) throws IOException {
        return listAllPermissions(userDriveService, fileId).stream()
            .filter(permission -> "user".equals(permission.getType()))
            .filter(permission -> serviceAccountEmail.equals(permission.getEmailAddress()))
            .findFirst()
            .orElse(null);
    }

    private static PermissionApplyStatus applyServiceAccountPermission(
        Drive userDriveService,
        String fileId,
        String targetRole,
        String serviceAccountEmail
    ) throws IOException {
        validateTargetRole(targetRole);
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
                "Service account {} access granted. fileId={}, email={}",
                targetRole,
                fileId,
                serviceAccountEmail
            );
            return PermissionApplyStatus.CREATED;
        }

        String currentRole = existingPermission.getRole();
        if (hasRequiredRole(currentRole, targetRole)) {
            log.info(
                "Service account permission already satisfies requested role. fileId={}, role={}, email={}",
                fileId,
                currentRole,
                serviceAccountEmail
            );
            return PermissionApplyStatus.UNCHANGED;
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
        return PermissionApplyStatus.UPGRADED;
    }

    private static PermissionApplyStatus recoverPermissionApplyStatus(
        Drive userDriveService,
        String fileId,
        String serviceAccountEmail,
        String targetRole,
        Permission initialPermission,
        int attempt
    ) {
        try {
            Permission currentPermission = findServiceAccountPermission(
                userDriveService,
                fileId,
                serviceAccountEmail
            );
            if (currentPermission == null
                || !hasRequiredRole(currentPermission.getRole(), targetRole)) {
                return null;
            }

            if (initialPermission == null) {
                return PermissionApplyStatus.CREATED;
            }

            return hasRequiredRole(initialPermission.getRole(), targetRole)
                ? PermissionApplyStatus.UNCHANGED
                : PermissionApplyStatus.UPGRADED;
        } catch (IOException e) {
            log.debug(
                "Failed to re-check service account permission after attempt {}. fileId={}, email={}, cause={}",
                attempt,
                fileId,
                serviceAccountEmail,
                e.getMessage()
            );
            return null;
        }
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
