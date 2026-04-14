package gg.agit.konect.integration.admin.advertisement;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementCreateRequest;
import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementUpdateRequest;
import gg.agit.konect.domain.advertisement.model.Advertisement;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.AdvertisementFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class AdminAdvertisementApiTest extends IntegrationTestSupport {

    private static final String ADMIN_ADVERTISEMENTS_ENDPOINT = "/admin/advertisements";

    private User adminUser;
    private User normalUser;

    @BeforeEach
    void setUp() throws Exception {
        University university = persist(UniversityFixture.create());
        adminUser = persist(UserFixture.createAdmin(university));
        normalUser = persist(UserFixture.createUser(university, "일반유저", "2021136001"));
        clearPersistenceContext();
    }

    @Nested
    @DisplayName("GET /admin/advertisements - 광고 목록 조회")
    class GetAdvertisements {

        @Test
        @DisplayName("광고 목록을 최신순으로 조회한다")
        void getAdvertisementsSuccess() throws Exception {
            // given
            mockLoginUser(adminUser.getId());
            Advertisement first = persist(AdvertisementFixture.create("첫 번째 광고", true));
            Advertisement second = persist(AdvertisementFixture.create("두 번째 광고", false));
            clearPersistenceContext();

            // when & then
            performGet(ADMIN_ADVERTISEMENTS_ENDPOINT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advertisements", hasSize(2)))
                .andExpect(jsonPath("$.advertisements[0].id").value(second.getId()))
                .andExpect(jsonPath("$.advertisements[1].id").value(first.getId()));
        }

        @Test
        @DisplayName("광고가 없으면 빈 목록을 반환한다")
        void getAdvertisementsWhenEmpty() throws Exception {
            // given
            mockLoginUser(adminUser.getId());

            // when & then
            performGet(ADMIN_ADVERTISEMENTS_ENDPOINT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advertisements", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /admin/advertisements/{id} - 광고 단건 조회")
    class GetAdvertisement {

        @Test
        @DisplayName("광고 단건을 조회한다")
        void getAdvertisementSuccess() throws Exception {
            // given
            mockLoginUser(adminUser.getId());
            Advertisement advertisement = persist(AdvertisementFixture.create("광고 제목", true));
            clearPersistenceContext();

            // when & then
            performGet(ADMIN_ADVERTISEMENTS_ENDPOINT + "/" + advertisement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(advertisement.getId()))
                .andExpect(jsonPath("$.title").value("광고 제목"))
                .andExpect(jsonPath("$.isVisible").value(true));
        }

        @Test
        @DisplayName("존재하지 않는 광고면 404를 반환한다")
        void getAdvertisementNotFound() throws Exception {
            // given
            mockLoginUser(adminUser.getId());

            // when & then
            performGet(ADMIN_ADVERTISEMENTS_ENDPOINT + "/99999")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_ADVERTISEMENT.getCode()));
        }
    }

    @Nested
    @DisplayName("POST /admin/advertisements - 광고 생성")
    class CreateAdvertisement {

        @Test
        @DisplayName("광고를 생성한다")
        void createAdvertisementSuccess() throws Exception {
            // given
            mockLoginUser(adminUser.getId());

            // when & then
            performPost(ADMIN_ADVERTISEMENTS_ENDPOINT, createRequest("생성 광고", true))
                .andExpect(status().isOk());

            performGet(ADMIN_ADVERTISEMENTS_ENDPOINT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advertisements", hasSize(1)))
                .andExpect(jsonPath("$.advertisements[0].title").value("생성 광고"))
                .andExpect(jsonPath("$.advertisements[0].isVisible").value(true))
                .andExpect(jsonPath("$.advertisements[0].clickCount").value(0));
        }

        @Test
        @DisplayName("광고 제목이 비어 있으면 400을 반환한다")
        void createAdvertisementInvalidBodyFails() throws Exception {
            // given
            mockLoginUser(adminUser.getId());
            AdminAdvertisementCreateRequest request = new AdminAdvertisementCreateRequest(
                " ",
                "광고 설명",
                "https://example.com/image.png",
                "https://example.com/link",
                true
            );

            // when & then
            performPost(ADMIN_ADVERTISEMENTS_ENDPOINT, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_REQUEST_BODY.getCode()));
        }

        @Test
        @DisplayName("어드민 권한이 없으면 403을 반환한다")
        void createAdvertisementForbidden() throws Exception {
            // given
            mockLoginUser(normalUser.getId());
            given(authorizationInterceptor.preHandle(any(), any(), any()))
                .willThrow(CustomException.of(ApiResponseCode.FORBIDDEN_ROLE_ACCESS));

            // when & then
            performPost(ADMIN_ADVERTISEMENTS_ENDPOINT, createRequest("권한 없음", true))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.FORBIDDEN_ROLE_ACCESS.getCode()));
        }
    }

    @Nested
    @DisplayName("PUT /admin/advertisements/{id} - 광고 수정")
    class UpdateAdvertisement {

        @Test
        @DisplayName("광고를 수정한다")
        void updateAdvertisementSuccess() throws Exception {
            // given
            mockLoginUser(adminUser.getId());
            Advertisement advertisement = persist(AdvertisementFixture.create("기존 광고", true));
            clearPersistenceContext();

            AdminAdvertisementUpdateRequest request = new AdminAdvertisementUpdateRequest(
                "수정 광고",
                "수정 설명",
                "https://example.com/new-image.png",
                "https://example.com/new-link",
                false
            );

            // when & then
            performPut(ADMIN_ADVERTISEMENTS_ENDPOINT + "/" + advertisement.getId(), request)
                .andExpect(status().isOk());

            performGet(ADMIN_ADVERTISEMENTS_ENDPOINT + "/" + advertisement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정 광고"))
                .andExpect(jsonPath("$.description").value("수정 설명"))
                .andExpect(jsonPath("$.isVisible").value(false));
        }

        @Test
        @DisplayName("수정 대상 광고가 없으면 404를 반환한다")
        void updateAdvertisementNotFound() throws Exception {
            // given
            mockLoginUser(adminUser.getId());
            AdminAdvertisementUpdateRequest request = new AdminAdvertisementUpdateRequest(
                "수정 광고",
                "수정 설명",
                "https://example.com/new-image.png",
                "https://example.com/new-link",
                false
            );

            // when & then
            performPut(ADMIN_ADVERTISEMENTS_ENDPOINT + "/99999", request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_ADVERTISEMENT.getCode()));
        }

        @Test
        @DisplayName("광고 수정은 기존 클릭 수를 보존한다")
        void updateAdvertisementPreservesClickCount() throws Exception {
            // given
            mockLoginUser(adminUser.getId());
            Integer advertisementId = insertAdvertisement("기존 광고", true, 7);
            clearPersistenceContext();

            AdminAdvertisementUpdateRequest request = new AdminAdvertisementUpdateRequest(
                "수정 광고",
                "수정 설명",
                "https://example.com/new-image.png",
                "https://example.com/new-link",
                false
            );

            // when & then
            performPut(ADMIN_ADVERTISEMENTS_ENDPOINT + "/" + advertisementId, request)
                .andExpect(status().isOk());

            performGet(ADMIN_ADVERTISEMENTS_ENDPOINT + "/" + advertisementId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount").value(7));
        }
    }

    @Nested
    @DisplayName("DELETE /admin/advertisements/{id} - 광고 삭제")
    class DeleteAdvertisement {

        @Test
        @DisplayName("광고를 삭제한다")
        void deleteAdvertisementSuccess() throws Exception {
            // given
            mockLoginUser(adminUser.getId());
            Advertisement advertisement = persist(AdvertisementFixture.create("삭제 광고", true));
            clearPersistenceContext();

            // when & then
            performDelete(ADMIN_ADVERTISEMENTS_ENDPOINT + "/" + advertisement.getId())
                .andExpect(status().isOk());

            performGet(ADMIN_ADVERTISEMENTS_ENDPOINT + "/" + advertisement.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_ADVERTISEMENT.getCode()));
        }

        @Test
        @DisplayName("삭제 대상 광고가 없으면 404를 반환한다")
        void deleteAdvertisementNotFound() throws Exception {
            // given
            mockLoginUser(adminUser.getId());

            // when & then
            performDelete(ADMIN_ADVERTISEMENTS_ENDPOINT + "/99999")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_ADVERTISEMENT.getCode()));
        }
    }

    private AdminAdvertisementCreateRequest createRequest(String title, boolean isVisible) {
        return new AdminAdvertisementCreateRequest(
            title,
            title + " 설명",
            "https://example.com/image.png",
            "https://example.com/link",
            isVisible
        );
    }

    private Integer insertAdvertisement(String title, boolean isVisible, int clickCount) {
        entityManager.createNativeQuery("""
                insert into advertisement (
                    title, description, image_url, link_url, is_visible, click_count, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """)
            .setParameter(1, title)
            .setParameter(2, title + " 설명")
            .setParameter(3, "https://example.com/image.png")
            .setParameter(4, "https://example.com/link")
            .setParameter(5, isVisible)
            .setParameter(6, clickCount)
            .executeUpdate();

        entityManager.flush();
        return ((Number)entityManager.createNativeQuery("select max(id) from advertisement")
            .getSingleResult()).intValue();
    }
}
