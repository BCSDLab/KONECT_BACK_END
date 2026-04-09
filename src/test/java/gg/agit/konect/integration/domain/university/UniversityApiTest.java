package gg.agit.konect.integration.domain.university;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.domain.university.enums.Campus;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;

class UniversityApiTest extends IntegrationTestSupport {

    private static final String UNIVERSITIES_ENDPOINT = "/universities";

    @Nested
    @DisplayName("GET /universities - 대학 목록 조회")
    class GetUniversities {

        @Test
        @DisplayName("대학 목록을 이름 오름차순으로 조회한다")
        void getUniversitiesSuccess() throws Exception {
            // given
            persist(UniversityFixture.create("한국기술교육대학교", Campus.MAIN));
            persist(UniversityFixture.create("가나다대학교", Campus.MAIN));
            persist(UniversityFixture.create("서울대학교", Campus.MAIN));
            clearPersistenceContext();

            // when & then
            performGet(UNIVERSITIES_ENDPOINT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.universities", hasSize(3)))
                .andExpect(jsonPath("$.universities[0].name").value("가나다대학교"))
                .andExpect(jsonPath("$.universities[1].name").value("서울대학교"))
                .andExpect(jsonPath("$.universities[2].name").value("한국기술교육대학교"));
        }

        @Test
        @DisplayName("대학이 없으면 빈 목록을 반환한다")
        void getUniversitiesWhenEmpty() throws Exception {
            // when & then
            performGet(UNIVERSITIES_ENDPOINT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.universities", hasSize(0)));
        }
    }
}
