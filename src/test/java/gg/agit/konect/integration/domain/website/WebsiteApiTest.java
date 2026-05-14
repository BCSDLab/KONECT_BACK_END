package gg.agit.konect.integration.domain.website;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.university.enums.Campus;
import gg.agit.konect.domain.university.enums.UniversityRegion;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.ClubRecruitmentFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class WebsiteApiTest extends IntegrationTestSupport {

    @Nested
    @DisplayName("GET /website/home - 웹사이트 메인")
    class GetHome {

        @Test
        @DisplayName("로그인 없이 대학 목록과 등록 동아리 수를 조회한다")
        void getHomeWithoutLogin() throws Exception {
            // given
            University koreatech = persist(UniversityFixture.create(
                "한국기술교육대학교",
                Campus.MAIN,
                UniversityRegion.CHUNGCHEONG
            ));
            University seoul = persist(UniversityFixture.create(
                "서울대학교",
                Campus.MAIN,
                UniversityRegion.SEOUL
            ));
            persist(createClub(koreatech, "BCSD Lab", ClubCategory.ACADEMIC));
            persist(createClub(koreatech, "COK", ClubCategory.SPORTS));
            persist(createClub(seoul, "서울 동아리", ClubCategory.HOBBY));
            clearPersistenceContext();

            // when & then
            performGet("/website/home?query=한국&region=CHUNGCHEONG")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUniversityCount").value(1))
                .andExpect(jsonPath("$.universities[0].name").value("한국기술교육대학교"))
                .andExpect(jsonPath("$.universities[0].campusName").value("본교"))
                .andExpect(jsonPath("$.universities[0].region").value("CHUNGCHEONG"))
                .andExpect(jsonPath("$.universities[0].regionName").value("충청도"))
                .andExpect(jsonPath("$.universities[0].clubCount").value(2));

            verify(loginCheckInterceptor, never()).preHandle(any(), any(), any());
            verify(authorizationInterceptor, never()).preHandle(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("GET /website/universities/{universityId}/clubs - 대학별 동아리")
    class GetUniversityClubs {

        @Test
        @DisplayName("검색어와 분과로 동아리 목록을 조회하고 분과별 개수를 함께 반환한다")
        void getUniversityClubsWithFilters() throws Exception {
            // given
            University university = persist(UniversityFixture.create(
                "한국기술교육대학교",
                Campus.MAIN,
                UniversityRegion.CHUNGCHEONG
            ));
            Club bcsd = persist(createClub(university, "BCSD Lab", ClubCategory.ACADEMIC));
            Club study = persist(createClub(university, "경영전략연구회", ClubCategory.ACADEMIC));
            persist(createClub(university, "ZEST", ClubCategory.PERFORMANCE));
            persistMember(bcsd, "회원1", "2024000001");
            persistMember(bcsd, "회원2", "2024000002");
            withdraw(persistMember(bcsd, "탈퇴회원", "2024000004"));
            persistMember(study, "회원3", "2024000003");
            clearPersistenceContext();

            // when & then
            performGet("/website/universities/" + university.getId()
                + "/clubs?page=1&limit=10&query=BCSD&category=ACADEMIC")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.university.name").value("한국기술교육대학교"))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.clubs", hasSize(1)))
                .andExpect(jsonPath("$.clubs[0].name").value("BCSD Lab"))
                .andExpect(jsonPath("$.clubs[0].memberCount").value(2))
                .andExpect(jsonPath("$.categories[0].category").value("ACADEMIC"))
                .andExpect(jsonPath("$.categories[0].count").value(1));
        }

        @Test
        @DisplayName("존재하지 않는 대학이면 404를 반환한다")
        void getUniversityClubsNotFound() throws Exception {
            // when & then
            performGet("/website/universities/99999/clubs")
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /website/clubs/{clubId} - 동아리 상세")
    class GetClubDetail {

        @Test
        @DisplayName("동아리 상세 소개와 모집 정보를 조회한다")
        void getClubDetailSuccess() throws Exception {
            // given
            University university = persist(UniversityFixture.create(
                "한국기술교육대학교",
                Campus.MAIN,
                UniversityRegion.CHUNGCHEONG
            ));
            Club club = persist(createClub(university, "ZEST", ClubCategory.PERFORMANCE));
            persist(ClubRecruitmentFixture.createAlwaysRecruiting(club));
            persistMember(club, "회장", "2024000004");
            clearPersistenceContext();

            // when & then
            performGet("/website/clubs/" + club.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ZEST"))
                .andExpect(jsonPath("$.categoryName").value("공연"))
                .andExpect(jsonPath("$.university.name").value("한국기술교육대학교"))
                .andExpect(jsonPath("$.university.region").value("CHUNGCHEONG"))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.recruitment.isAlwaysRecruiting").value(true))
                .andExpect(jsonPath("$.recruitment.content").value("상시 모집 공고 내용입니다."));
        }
    }

    @Nested
    @DisplayName("GET /website/clubs/recent - 최근 본 동아리")
    class GetRecentClubs {

        @Test
        @DisplayName("요청한 동아리 ID 순서대로 카드 정보를 반환한다")
        void getRecentClubsKeepsRequestOrder() throws Exception {
            // given
            University university = persist(UniversityFixture.create(
                "한국기술교육대학교",
                Campus.MAIN,
                UniversityRegion.CHUNGCHEONG
            ));
            Club first = persist(createClub(university, "첫 번째", ClubCategory.ACADEMIC));
            Club second = persist(createClub(university, "두 번째", ClubCategory.SPORTS));
            clearPersistenceContext();

            // when & then
            performGet("/website/clubs/recent?clubIds=" + second.getId() + "," + first.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubs", hasSize(2)))
                .andExpect(jsonPath("$.clubs[0].name").value("두 번째"))
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
            performGet("/website/clubs/recent?clubIds=" + clubIds)
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("최근 본 동아리 ID가 비어 있으면 400을 반환한다")
        void getRecentClubsRejectsEmptyClubIds() throws Exception {
            // when & then
            performGet("/website/clubs/recent?clubIds=")
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("최근 본 동아리 ID가 없으면 400을 반환한다")
        void getRecentClubsRejectsMissingClubIds() throws Exception {
            // when & then
            performGet("/website/clubs/recent")
                .andExpect(status().isBadRequest());
        }
    }

    private Club createClub(University university, String name, ClubCategory category) {
        return Club.builder()
            .university(university)
            .name(name)
            .description("한 줄 소개")
            .introduce("상세 소개입니다.")
            .imageUrl("https://example.com/" + name + ".png")
            .location("학생회관 101호")
            .clubCategory(category)
            .isRecruitmentEnabled(false)
            .isApplicationEnabled(false)
            .isFeeRequired(false)
            .build();
    }

    private User persistMember(Club club, String name, String studentNumber) {
        User user = persist(UserFixture.createUser(club.getUniversity(), name, studentNumber));
        persist(ClubMemberFixture.createMember(club, user));
        return user;
    }

    private void withdraw(User user) {
        user.withdraw(LocalDateTime.now());
    }
}
