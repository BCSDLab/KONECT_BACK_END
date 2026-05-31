package gg.agit.konect.integration.domain.website;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.university.enums.Campus;
import gg.agit.konect.domain.university.enums.UniversityRegion;
import gg.agit.konect.domain.website.model.WebClub;
import gg.agit.konect.domain.website.model.WebUniversity;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.WebClubFixture;
import gg.agit.konect.support.fixture.WebUniversityFixture;

class WebsiteApiTest extends IntegrationTestSupport {

    @Nested
    @DisplayName("GET /konect/home - 웹사이트 메인")
    class GetHome {

        @Test
        @DisplayName("로그인 없이 대학 목록과 등록 동아리 수를 조회한다")
        void getHomeWithoutLogin() throws Exception {
            // given
            WebUniversity koreatech = persist(WebUniversityFixture.create(
                "한국기술교육대학교",
                Campus.MAIN,
                UniversityRegion.CHUNGCHEONG,
                "https://example.com/koreatech-logo.png"
            ));
            WebUniversity seoul = persist(WebUniversityFixture.create(
                "서울대학교",
                Campus.MAIN,
                UniversityRegion.SEOUL
            ));
            persist(WebClubFixture.create(koreatech, "BCSD Lab", ClubCategory.ACADEMIC));
            persist(WebClubFixture.create(koreatech, "COK", ClubCategory.SPORTS));
            persist(WebClubFixture.create(seoul, "서울 동아리", ClubCategory.HOBBY));
            clearPersistenceContext();

            // when & then
            performGet("/konect/home?query=한국&region=CHUNGCHEONG")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUniversityCount").value(1))
                .andExpect(jsonPath("$.regions", hasSize(8)))
                .andExpect(jsonPath("$.regions[0].region").value("GANGWON"))
                .andExpect(jsonPath("$.regions[1].region").value("GYEONGGI"))
                .andExpect(jsonPath("$.regions[2].region").value("GYEONGSANG"))
                .andExpect(jsonPath("$.regions[3].region").value("SEOUL"))
                .andExpect(jsonPath("$.regions[4].region").value("JEOLLA"))
                .andExpect(jsonPath("$.regions[5].region").value("JEJU"))
                .andExpect(jsonPath("$.regions[6].region").value("CHUNGCHEONG"))
                .andExpect(jsonPath("$.regions[7].region").value("UNKNOWN"))
                .andExpect(jsonPath("$.universities[0].name").value("한국기술교육대학교"))
                .andExpect(jsonPath("$.universities[0].campusName").value("본교"))
                .andExpect(jsonPath("$.universities[0].region").value("CHUNGCHEONG"))
                .andExpect(jsonPath("$.universities[0].regionName").value("충청도"))
                .andExpect(jsonPath("$.universities[0].imageUrl").value("https://example.com/koreatech-logo.png"))
                .andExpect(jsonPath("$.universities[0].clubCount").value(2));

            verify(loginCheckInterceptor, never()).preHandle(any(), any(), any());
            verify(authorizationInterceptor, never()).preHandle(any(), any(), any());
        }

        @Test
        @DisplayName("대학교 이름 초성과 약칭으로 웹사이트 대학 목록을 검색한다")
        void getHomeSearchesUniversitiesByChoseongAndAlias() throws Exception {
            // given
            persist(WebUniversityFixture.create(
                "한국기술교육대학교",
                Campus.MAIN,
                UniversityRegion.CHUNGCHEONG,
                "https://example.com/koreatech-logo.png"
            ));
            persist(WebUniversityFixture.create(
                "서울과학기술대학교",
                Campus.MAIN,
                UniversityRegion.SEOUL
            ));
            clearPersistenceContext();

            // when & then
            performGet("/konect/home?query=ㅎㄱㄳㄱㅇㄷㅎㄱ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUniversityCount").value(1))
                .andExpect(jsonPath("$.universities[0].name").value("한국기술교육대학교"));

            performGet("/konect/home?query=한기대")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUniversityCount").value(1))
                .andExpect(jsonPath("$.universities[0].name").value("한국기술교육대학교"));

            performGet("/konect/home?query=과기대")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUniversityCount").value(1))
                .andExpect(jsonPath("$.universities[0].name").value("서울과학기술대학교"));
        }
    }

    @Nested
    @DisplayName("GET /konect/universities/{universityId}/clubs - 대학별 동아리")
    class GetUniversityClubs {

        @Test
        @DisplayName("검색어와 분과로 동아리 목록을 조회하고 대학 전체 동아리 수를 함께 반환한다")
        void getUniversityClubsWithFilters() throws Exception {
            // given
            WebUniversity university = persist(WebUniversityFixture.create(
                "한국기술교육대학교",
                Campus.MAIN,
                UniversityRegion.CHUNGCHEONG,
                "https://example.com/koreatech-logo.png"
            ));
            persist(WebClubFixture.create(university, "BCSD Lab", ClubCategory.ACADEMIC));
            persist(WebClubFixture.create(university, "경영전략연구회", ClubCategory.ACADEMIC));
            persist(WebClubFixture.create(university, "ZEST", ClubCategory.PERFORMANCE));
            clearPersistenceContext();

            // when & then
            performGet("/konect/universities/" + university.getId()
                + "/clubs?page=1&limit=10&query=BCSD&category=ACADEMIC")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.university.name").value("한국기술교육대학교"))
                .andExpect(jsonPath("$.university.imageUrl").value("https://example.com/koreatech-logo.png"))
                .andExpect(jsonPath("$.university.clubCount").value(3))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.clubs", hasSize(1)))
                .andExpect(jsonPath("$.clubs[0].name").value("BCSD Lab"))
                .andExpect(jsonPath("$.clubs[0].categoryEmoji").value("📚"))
                .andExpect(jsonPath("$.categories[0].category").value("PERFORMANCE"))
                .andExpect(jsonPath("$.categories[1].category").value("SOCIAL_SERVICE"))
                .andExpect(jsonPath("$.categories[2].category").value("EXHIBITION_CREATION"))
                .andExpect(jsonPath("$.categories[3].category").value("RELIGION"))
                .andExpect(jsonPath("$.categories[4].category").value("SPORTS"))
                .andExpect(jsonPath("$.categories[5].category").value("HOBBY"))
                .andExpect(jsonPath("$.categories[6].category").value("ACADEMIC"))
                .andExpect(jsonPath("$.categories[?(@.category == 'ACADEMIC')].count")
                    .value(contains(1)))
                .andExpect(jsonPath("$.categories[7].category").value("ETC"));
        }

        @Test
        @DisplayName("sortBy=CATEGORY이면 분과 표시 순서와 동아리명 순서로 정렬한다")
        void getUniversityClubsSortedByCategory() throws Exception {
            // given
            WebUniversity university = persist(WebUniversityFixture.create(
                "Koreatech",
                Campus.MAIN,
                UniversityRegion.CHUNGCHEONG
            ));
            persist(WebClubFixture.create(university, "Zeta", ClubCategory.ACADEMIC));
            persist(WebClubFixture.create(university, "Alpha", ClubCategory.ACADEMIC));
            persist(WebClubFixture.create(university, "Runner", ClubCategory.SPORTS));
            persist(WebClubFixture.create(university, "Band", ClubCategory.PERFORMANCE));
            clearPersistenceContext();

            // when & then
            performGet("/konect/universities/" + university.getId() + "/clubs?sortBy=CATEGORY")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubs[*].name", contains("Band", "Runner", "Alpha", "Zeta")))
                .andExpect(jsonPath("$.clubs[*].category", contains(
                    "PERFORMANCE",
                    "SPORTS",
                    "ACADEMIC",
                    "ACADEMIC"
                )));
        }

        @Test
        @DisplayName("존재하지 않는 대학이면 404를 반환한다")
        void getUniversityClubsNotFound() throws Exception {
            // when & then
            performGet("/konect/universities/99999/clubs")
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /konect/clubs/{clubId} - 동아리 상세")
    class GetClubDetail {

        @Test
        @DisplayName("동아리 상세 소개를 조회한다")
        void getClubDetailSuccess() throws Exception {
            // given
            WebUniversity university = persist(WebUniversityFixture.create(
                "한국기술교육대학교",
                Campus.MAIN,
                UniversityRegion.CHUNGCHEONG,
                "https://example.com/koreatech-logo.png"
            ));
            WebClub club = persist(WebClubFixture.create(university, "ZEST", ClubCategory.PERFORMANCE));
            persist(WebClubFixture.create(university, "BCSD Lab", ClubCategory.ACADEMIC));
            clearPersistenceContext();

            // when & then
            performGet("/konect/clubs/" + club.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ZEST"))
                .andExpect(jsonPath("$.categoryEmoji").value("📚"))
                .andExpect(jsonPath("$.categoryName").value("공연"))
                .andExpect(jsonPath("$.topic").value("코딩"))
                .andExpect(jsonPath("$.university.name").value("한국기술교육대학교"))
                .andExpect(jsonPath("$.university.region").value("CHUNGCHEONG"))
                .andExpect(jsonPath("$.university.imageUrl").value("https://example.com/koreatech-logo.png"))
                .andExpect(jsonPath("$.university.clubCount").value(2));
        }
    }

    @Nested
    @DisplayName("GET /konect/clubs/recent - 최근 본 동아리")
    class GetRecentClubs {

        @Test
        @DisplayName("요청한 동아리 ID 순서대로 카드 정보를 반환한다")
        void getRecentClubsKeepsRequestOrder() throws Exception {
            // given
            WebUniversity university = persist(WebUniversityFixture.create(
                "한국기술교육대학교",
                Campus.MAIN,
                UniversityRegion.CHUNGCHEONG
            ));
            WebClub first = persist(WebClubFixture.create(university, "첫 번째", ClubCategory.ACADEMIC));
            WebClub second = persist(WebClubFixture.create(university, "두 번째", ClubCategory.SPORTS));
            clearPersistenceContext();

            // when & then
            performGet("/konect/clubs/recent?clubIds=" + second.getId() + "," + first.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubs", hasSize(2)))
                .andExpect(jsonPath("$.clubs[0].name").value("두 번째"))
                .andExpect(jsonPath("$.clubs[0].categoryEmoji").value("📚"))
                .andExpect(jsonPath("$.clubs[1].name").value("첫 번째"));
        }

        @Test
        @DisplayName("최근 본 동아리 ID가 100개를 초과하면 400을 반환한다")
        void getRecentClubsRejectsTooManyClubIds() throws Exception {
            // given
            String clubIds = IntStream.rangeClosed(1, 101)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));

            // when & then
            performGet("/konect/clubs/recent?clubIds=" + clubIds)
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("최근 본 동아리 ID가 비어 있으면 400을 반환한다")
        void getRecentClubsRejectsEmptyClubIds() throws Exception {
            // when & then
            performGet("/konect/clubs/recent?clubIds=")
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("최근 본 동아리 ID가 없으면 400을 반환한다")
        void getRecentClubsRejectsMissingClubIds() throws Exception {
            // when & then
            performGet("/konect/clubs/recent")
                .andExpect(status().isBadRequest());
        }
    }
}
