package gg.agit.konect.integration.domain.bank;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.support.IntegrationTestSupport;

class BankApiTest extends IntegrationTestSupport {

    private static final String BANKS_ENDPOINT = "/banks";

    @Nested
    @DisplayName("GET /banks - 은행 목록 조회")
    class GetBanks {

        @Test
        @DisplayName("등록된 은행 목록을 조회한다")
        void getBanksSuccess() throws Exception {
            // given
            insertBank("국민은행", "https://example.com/kb.png");
            insertBank("카카오뱅크", "https://example.com/kakao.png");
            clearPersistenceContext();

            // when & then
            performGet(BANKS_ENDPOINT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banks", hasSize(2)))
                .andExpect(jsonPath("$.banks[0].name").value("국민은행"))
                .andExpect(jsonPath("$.banks[1].name").value("카카오뱅크"));
        }

        @Test
        @DisplayName("등록된 은행이 없으면 빈 목록을 반환한다")
        void getBanksWhenEmpty() throws Exception {
            // when & then
            performGet(BANKS_ENDPOINT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banks", hasSize(0)));
        }
    }

    private void insertBank(String name, String imageUrl) {
        entityManager.createNativeQuery("""
                insert into bank (name, image_url, created_at, updated_at)
                values (?, ?, current_timestamp, current_timestamp)
                """)
            .setParameter(1, name)
            .setParameter(2, imageUrl)
            .executeUpdate();
    }
}
