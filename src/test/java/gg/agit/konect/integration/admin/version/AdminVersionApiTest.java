package gg.agit.konect.integration.admin.version;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.admin.version.dto.AdminVersionCreateRequest;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.version.enums.PlatformType;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class AdminVersionApiTest extends IntegrationTestSupport {

    private static final String ADMIN_VERSIONS_ENDPOINT = "/admin/versions";
    private static final String LATEST_VERSION_ENDPOINT = "/versions/latest";

    private User adminUser;
    private User normalUser;

    @BeforeEach
    void setUp() {
        University university = persist(UniversityFixture.create());
        adminUser = persist(UserFixture.createAdmin(university));
        normalUser = persist(UserFixture.createUser(university, "일반유저", "2021136001"));
    }

    @Nested
    @DisplayName("POST /admin/versions - 버전 등록")
    class CreateVersion {

        @Test
        @DisplayName("관리자가 새 버전을 등록한다")
        void createVersionSuccess() throws Exception {
            // given
            mockLoginUser(adminUser.getId());
            AdminVersionCreateRequest request = new AdminVersionCreateRequest(
                PlatformType.IOS, "1.2.3", "새 기능 추가"
            );

            // when & then
            performPost(ADMIN_VERSIONS_ENDPOINT, request)
                .andExpect(status().isOk());

            performGet(LATEST_VERSION_ENDPOINT + "?platform=IOS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.2.3"))
                .andExpect(jsonPath("$.releaseNotes").value("새 기능 추가"));
        }

        @Test
        @DisplayName("같은 플랫폼/버전을 중복 등록하면 409를 반환한다")
        void createVersionDuplicateFails() throws Exception {
            // given
            mockLoginUser(adminUser.getId());
            AdminVersionCreateRequest request = createRequest(PlatformType.ANDROID, "2.0.0");

            performPost(ADMIN_VERSIONS_ENDPOINT, request)
                .andExpect(status().isOk());

            // when & then
            performPost(ADMIN_VERSIONS_ENDPOINT, request)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.DUPLICATE_VERSION.getCode()));
        }

        @Test
        @DisplayName("버전 값이 비어 있으면 400을 반환한다")
        void createVersionWithBlankVersionFails() throws Exception {
            // given
            mockLoginUser(adminUser.getId());
            AdminVersionCreateRequest request = createRequest(PlatformType.IOS, " ");

            // when & then
            performPost(ADMIN_VERSIONS_ENDPOINT, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_REQUEST_BODY.getCode()));
        }

        @Test
        @DisplayName("관리자 권한이 없으면 403을 반환한다")
        void createVersionWithoutAdminRoleFails() throws Exception {
            // given
            mockLoginUser(normalUser.getId());
            given(authorizationInterceptor.preHandle(any(), any(), any()))
                .willThrow(CustomException.of(ApiResponseCode.FORBIDDEN_ROLE_ACCESS));

            AdminVersionCreateRequest request = createRequest(PlatformType.IOS, "1.0.0");

            // when & then
            performPost(ADMIN_VERSIONS_ENDPOINT, request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.FORBIDDEN_ROLE_ACCESS.getCode()));
        }

        @Test
        @DisplayName("같은 버전 문자열이라도 플랫폼이 다르면 중복이 아니다")
        void createVersionWithSameVersionDifferentPlatformSucceeds() throws Exception {
            // given
            mockLoginUser(adminUser.getId());

            // when & then
            performPost(ADMIN_VERSIONS_ENDPOINT, createRequest(PlatformType.IOS, "3.0.0"))
                .andExpect(status().isOk());

            performPost(ADMIN_VERSIONS_ENDPOINT, createRequest(PlatformType.ANDROID, "3.0.0"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("플랫폼이 없으면 400을 반환한다")
        void createVersionWithoutPlatformFails() throws Exception {
            // given
            mockLoginUser(adminUser.getId());
            AdminVersionCreateRequest request = new AdminVersionCreateRequest(null, "1.0.0", "릴리즈 노트");

            // when & then
            performPost(ADMIN_VERSIONS_ENDPOINT, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_REQUEST_BODY.getCode()));
        }
    }

    private AdminVersionCreateRequest createRequest(PlatformType platform, String version) {
        return new AdminVersionCreateRequest(platform, version, "릴리즈 노트");
    }
}
