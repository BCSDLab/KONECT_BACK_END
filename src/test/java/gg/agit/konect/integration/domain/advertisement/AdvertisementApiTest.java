package gg.agit.konect.integration.domain.advertisement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementCreateRequest;
import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementUpdateRequest;
import gg.agit.konect.domain.advertisement.dto.AdvertisementResponse;
import gg.agit.konect.domain.advertisement.dto.AdvertisementsResponse;
import gg.agit.konect.domain.advertisement.model.Advertisement;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.AdvertisementFixture;

class AdvertisementApiTest extends IntegrationTestSupport {

    @Nested
    @DisplayName("GET /advertisements - 랜덤 광고 목록 조회")
    class GetAdvertisements {

        @Test
        @DisplayName("노출 가능한 광고를 랜덤으로 count개 조회한다 - 비노출 광고는 제외")
        void getRandomAdvertisements() throws Exception {
            // given
            persist(AdvertisementFixture.create("광고1", true));
            persist(AdvertisementFixture.create("광고2", true));
            persist(AdvertisementFixture.create("광고3", true));
            persist(AdvertisementFixture.create("비노출 광고", false));
            clearPersistenceContext();

            // when & then - 반환된 광고는 모두 노출 광고만 포함
            performGet("/advertisements?count=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advertisements", hasSize(2)))
                .andExpect(jsonPath("$.advertisements[*].title").value(org.hamcrest.Matchers.not("비노출 광고")));
        }

        @Test
        @DisplayName("count가 visible 광고 수 이하면 중복 없이 반환한다")
        void getRandomAdvertisementsNoDuplicate() throws Exception {
            // given
            persist(AdvertisementFixture.create("광고1", true));
            persist(AdvertisementFixture.create("광고2", true));
            persist(AdvertisementFixture.create("광고3", true));
            clearPersistenceContext();

            // when
            String responseJson = performGet("/advertisements?count=3")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advertisements", hasSize(3)))
                .andReturn()
                .getResponse()
                .getContentAsString();

            AdvertisementsResponse response = objectMapper.readValue(
                responseJson, AdvertisementsResponse.class);

            // then - 반환된 ID는 모두 distinct
            List<Integer> ids = response.advertisements().stream()
                .map(AdvertisementResponse::id)
                .toList();
            Set<Integer> uniqueIds = Set.copyOf(ids);
            assertThat(uniqueIds).hasSize(3);
        }

        @Test
        @DisplayName("count 기본값은 1이다")
        void getRandomAdvertisementsDefaultCount() throws Exception {
            // given
            persist(AdvertisementFixture.create("광고1", true));
            clearPersistenceContext();

            // when & then
            performGet("/advertisements")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advertisements", hasSize(1)));
        }

        @Test
        @DisplayName("count가 등록된 광고 수보다 많으면 중복을 허용하여 반환한다")
        void getRandomAdvertisementsWithDuplication() throws Exception {
            // given - 노출 광고 2개만 등록
            persist(AdvertisementFixture.create("광고1", true));
            persist(AdvertisementFixture.create("광고2", true));
            clearPersistenceContext();

            // when & then - 5개 요청 시 중복 허용하여 5개 반환
            performGet("/advertisements?count=5")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advertisements", hasSize(5)));
        }

        @Test
        @DisplayName("count가 1 미만이면 400을 반환한다")
        void getRandomAdvertisementsInvalidMinCount() throws Exception {
            performGet("/advertisements?count=0")
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("count가 10을 초과하면 400을 반환한다")
        void getRandomAdvertisementsInvalidMaxCount() throws Exception {
            performGet("/advertisements?count=11")
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("노출 가능한 광고가 없으면 빈 목록을 반환한다")
        void getRandomAdvertisementsEmpty() throws Exception {
            // given
            persist(AdvertisementFixture.create("비노출 광고", false));
            clearPersistenceContext();

            // when & then
            performGet("/advertisements?count=3")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advertisements", hasSize(0)));
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
