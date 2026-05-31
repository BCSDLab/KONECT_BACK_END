package gg.agit.konect.integration.domain.university;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.domain.university.enums.Campus;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.UniversitySearchKeywordFixture;
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

        @Test
        @DisplayName("동일한 이름이라도 캠퍼스가 다르면 각각 조회한다")
        void getUniversitiesWithSameNameDifferentCampus() throws Exception {
            // given
            persist(UniversityFixture.create("한국기술교육대학교", Campus.SECOND));
            persist(UniversityFixture.create("한국기술교육대학교", Campus.MAIN));
            clearPersistenceContext();

            // when & then
            performGet(UNIVERSITIES_ENDPOINT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.universities", hasSize(2)))
                .andExpect(jsonPath("$.universities[0].name").value("한국기술교육대학교"))
                .andExpect(jsonPath("$.universities[1].name").value("한국기술교육대학교"));
        }

        @Test
        @DisplayName("query가 대학교 이름 초성이면 일치하는 대학 목록을 조회한다")
        void getUniversitiesByChoseongQuery() throws Exception {
            // given
            persist(UniversityFixture.create("한국기술교육대학교", Campus.MAIN));
            persist(UniversityFixture.create("서울과학기술대학교", Campus.MAIN));
            persist(UniversityFixture.create("서울대학교", Campus.MAIN));
            clearPersistenceContext();

            // when & then
            performGet(UNIVERSITIES_ENDPOINT + "?query=ㅎㄱㄱㅅㄱㅇㄷㅎㄱ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.universities", hasSize(1)))
                .andExpect(jsonPath("$.universities[0].name").value("한국기술교육대학교"));
        }

        @Test
        @DisplayName("query 초성에 겹자음이 포함되어도 풀어서 검색한다")
        void getUniversitiesByChoseongQueryWithCompatibilityJamoCluster() throws Exception {
            // given
            persist(UniversityFixture.create("한국기술교육대학교", Campus.MAIN));
            persist(UniversityFixture.create("서울과학기술대학교", Campus.MAIN));
            clearPersistenceContext();

            // when & then
            performGet(UNIVERSITIES_ENDPOINT + "?query=ㅎㄱㄳㄱㅇㄷㅎㄱ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.universities", hasSize(1)))
                .andExpect(jsonPath("$.universities[0].name").value("한국기술교육대학교"));
        }

        @Test
        @DisplayName("query가 대학교 약칭이면 일치하는 대학 목록을 조회한다")
        void getUniversitiesByAliasQuery() throws Exception {
            // given
            University koreatech = persist(UniversityFixture.create("한국기술교육대학교", Campus.MAIN));
            University seoulTech = persist(UniversityFixture.create("서울과학기술대학교", Campus.MAIN));
            persist(UniversityFixture.create("서울대학교", Campus.MAIN));
            persist(UniversitySearchKeywordFixture.createAlias(koreatech, "한기대"));
            persist(UniversitySearchKeywordFixture.createAlias(seoulTech, "과기대"));
            clearPersistenceContext();

            // when & then
            performGet(UNIVERSITIES_ENDPOINT + "?query=한기대")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.universities", hasSize(1)))
                .andExpect(jsonPath("$.universities[0].name").value("한국기술교육대학교"));

            performGet(UNIVERSITIES_ENDPOINT + "?query=과기대")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.universities", hasSize(1)))
                .andExpect(jsonPath("$.universities[0].name").value("서울과학기술대학교"));
        }
    }
}
