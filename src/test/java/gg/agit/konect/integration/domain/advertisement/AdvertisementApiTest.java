package gg.agit.konect.integration.domain.advertisement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementCreateRequest;
import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementUpdateRequest;
import gg.agit.konect.domain.advertisement.model.Advertisement;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.AdvertisementFixture;

class AdvertisementApiTest extends IntegrationTestSupport {

    @Nested
    @DisplayName("GET /advertisements - 광고 목록 조회")
    class GetAdvertisements {

        @Test
        @DisplayName("노출 가능한 광고만 조회한다")
        void getVisibleAdvertisements() throws Exception {
            // given
            persist(AdvertisementFixture.create("노출 광고", true));
            persist(AdvertisementFixture.create("비노출 광고", false));
            clearPersistenceContext();

            // when & then
            performGet("/advertisements")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advertisements", hasSize(1)))
                .andExpect(jsonPath("$.advertisements[0].title").value("노출 광고"));
        }
    }

    @Nested
    @DisplayName("GET /advertisements/{id} - 광고 단건 조회")
    class GetAdvertisement {

        @Test
        @DisplayName("노출 가능한 광고 단건을 조회한다")
        void getVisibleAdvertisement() throws Exception {
            // given
            Advertisement advertisement = persist(AdvertisementFixture.create("단건 광고", true));
            clearPersistenceContext();

            // when & then
            performGet("/advertisements/" + advertisement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("단건 광고"));
        }

        @Test
        @DisplayName("비노출 광고 단건 조회 시 404를 반환한다")
        void getHiddenAdvertisement() throws Exception {
            // given
            Advertisement advertisement = persist(AdvertisementFixture.create("숨김 광고", false));
            clearPersistenceContext();

            // when & then
            performGet("/advertisements/" + advertisement.getId())
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /advertisements/{id}/clicks - 광고 클릭 수 증가")
    class IncreaseClickCount {

        @Test
        @DisplayName("광고 클릭 수를 1 증가시킨다")
        void increaseClickCount() throws Exception {
            // given
            Advertisement advertisement = persist(AdvertisementFixture.create("클릭 광고", true));
            clearPersistenceContext();

            // when & then
            performPost("/advertisements/" + advertisement.getId() + "/clicks")
                .andExpect(status().isNoContent());

            clearPersistenceContext();
            Advertisement foundAdvertisement = entityManager.find(Advertisement.class, advertisement.getId());
            assertThat(foundAdvertisement.getClickCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Admin Advertisement API")
    class AdminAdvertisementCrud {

        @Test
        @DisplayName("어드민이 광고를 생성한다")
        void createAdvertisement() throws Exception {
            // given
            AdminAdvertisementCreateRequest request = new AdminAdvertisementCreateRequest(
                "생성 광고",
                "생성 설명",
                "https://example.com/create.png",
                "https://example.com/create",
                true
            );

            // when & then
            performPost("/admin/advertisements", request)
                .andExpect(status().isOk());

            clearPersistenceContext();
            Long count = entityManager.createQuery("select count(a) from Advertisement a", Long.class)
                .getSingleResult();
            assertThat(count).isEqualTo(1L);
        }

        @Test
        @DisplayName("어드민이 광고 목록과 단건을 조회한다")
        void getAdvertisementsAndDetail() throws Exception {
            // given
            Advertisement advertisement = persist(AdvertisementFixture.create("관리 광고", true));
            clearPersistenceContext();

            // when & then
            performGet("/admin/advertisements")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advertisements", hasSize(1)))
                .andExpect(jsonPath("$.advertisements[0].title").value("관리 광고"));

            performGet("/admin/advertisements/" + advertisement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("관리 광고"));
        }

        @Test
        @DisplayName("어드민이 광고를 수정한다")
        void updateAdvertisement() throws Exception {
            // given
            Advertisement advertisement = persist(AdvertisementFixture.create("수정 전 광고", true));
            clearPersistenceContext();

            AdminAdvertisementUpdateRequest request = new AdminAdvertisementUpdateRequest(
                "수정 후 광고",
                "수정 설명",
                "https://example.com/update.png",
                "https://example.com/update",
                false
            );

            // when & then
            performPut("/admin/advertisements/" + advertisement.getId(), request)
                .andExpect(status().isOk());

            clearPersistenceContext();
            Advertisement foundAdvertisement = entityManager.find(Advertisement.class, advertisement.getId());
            assertThat(foundAdvertisement.getTitle()).isEqualTo("수정 후 광고");
            assertThat(foundAdvertisement.getIsVisible()).isFalse();
        }

        @Test
        @DisplayName("어드민이 광고를 삭제한다")
        void deleteAdvertisement() throws Exception {
            // given
            Advertisement advertisement = persist(AdvertisementFixture.create("삭제 광고", true));
            clearPersistenceContext();

            // when & then
            performDelete("/admin/advertisements/" + advertisement.getId())
                .andExpect(status().isOk());

            clearPersistenceContext();
            Advertisement foundAdvertisement = entityManager.find(Advertisement.class, advertisement.getId());
            assertThat(foundAdvertisement).isNull();
        }
    }
}
