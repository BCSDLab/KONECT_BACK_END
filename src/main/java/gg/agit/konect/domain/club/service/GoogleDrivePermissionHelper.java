package gg.agit.konect.domain.club.service;

final class GoogleDrivePermissionHelper {

    private static final int ROLE_RANK_NONE = 0;
    private static final int ROLE_RANK_READER = 1;
    private static final int ROLE_RANK_COMMENTER = 2;
    private static final int ROLE_RANK_WRITER = 3;

    private GoogleDrivePermissionHelper() {}

    enum PermissionApplyStatus {
        CREATED,
        UPGRADED,
        UNCHANGED
    }

    static boolean hasRequiredRole(String currentRole, String targetRole) {
        return roleRank(currentRole) >= roleRank(targetRole);
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
}
