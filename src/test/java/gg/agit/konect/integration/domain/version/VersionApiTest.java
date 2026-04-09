package gg.agit.konect.integration.domain.version;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.domain.version.enums.PlatformType;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.support.IntegrationTestSupport;

class VersionApiTest extends IntegrationTestSupport {

    private static final String LATEST_VERSION_ENDPOINT = "/versions/latest";

    @Nested
    @DisplayName("GET /versions/latest - 최신 버전 조회")
    class GetLatestVersion {

        @Test
        @DisplayName("플랫폼별 가장 최근 버전을 조회한다")
        void getLatestVersionSuccess() throws Exception {
            // given
            insertVersion(PlatformType.IOS, "1.0.0", "old release", LocalDateTime.of(2026, 1, 1, 0, 0));
            insertVersion(PlatformType.IOS, "1.2.0", "latest release", LocalDateTime.of(2026, 2, 1, 0, 0));
            insertVersion(PlatformType.ANDROID, "9.9.9", "android release", LocalDateTime.of(2026, 3, 1, 0, 0));
            clearPersistenceContext();

            // when & then
            performGet(LATEST_VERSION_ENDPOINT + "?platform=IOS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platform").value("IOS"))
                .andExpect(jsonPath("$.version").value("1.2.0"))
                .andExpect(jsonPath("$.releaseNotes").value("latest release"));
        }

        @Test
        @DisplayName("등록된 버전이 없으면 404를 반환한다")
        void getLatestVersionWhenMissing() throws Exception {
            // when & then
            performGet(LATEST_VERSION_ENDPOINT + "?platform=ANDROID")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_VERSION.getCode()));
        }

        @Test
        @DisplayName("지원하지 않는 플랫폼 값이면 400을 반환한다")
        void getLatestVersionWithInvalidPlatform() throws Exception {
            // when & then
            performGet(LATEST_VERSION_ENDPOINT + "?platform=WINDOWS")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_TYPE_VALUE.getCode()));
        }
    }

    private void insertVersion(PlatformType platform, String version, String releaseNotes, LocalDateTime createdAt) {
        entityManager.createNativeQuery("""
            insert into version (platform, version, release_notes, created_at, updated_at)
            values (?, ?, ?, ?, ?)
            """)
            .setParameter(1, platform.name())
            .setParameter(2, version)
            .setParameter(3, releaseNotes)
            .setParameter(4, createdAt)
            .setParameter(5, createdAt)
            .executeUpdate();
    }
}
