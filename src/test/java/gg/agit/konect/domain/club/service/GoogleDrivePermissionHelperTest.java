package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GoogleDrivePermissionHelperTest {

    @Test
    @DisplayName("throws when target role is unsupported")
    void hasRequiredRoleThrowsWhenTargetRoleIsUnsupported() {
        assertThatThrownBy(() -> GoogleDrivePermissionHelper.hasRequiredRole("reader", "invalid-role"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported targetRole");
    }
}
