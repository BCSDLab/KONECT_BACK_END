package gg.agit.konect.domain.club.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.domain.club.dto.ClubCreateRequest;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class ClubBasicControllerIntegrationTest extends IntegrationTestSupport {

    private University university;
    private User normalUser;
    private User adminUser;

    @BeforeEach
    void setUp() throws Exception {
        university = persist(UniversityFixture.create());
        normalUser = persist(UserFixture.createUser(university, "일반유저", "2021136001"));
        adminUser = persist(UserFixture.createAdmin(university));
    }

    @Nested
    @DisplayName("GET /clubs - 동아리 목록 조회")
    class GetClubs {

        @Test
        @DisplayName("동아리 목록을 조회한다")
        void getClubsSuccess() throws Exception {
            // given
            Club club1 = persist(ClubFixture.create(university, "BCSD Lab"));
            Club club2 = persist(ClubFixture.create(university, "스타트업 동아리"));
            User president1 = persist(UserFixture.createUser(university, "회장1", "2020000001"));
            User president2 = persist(UserFixture.createUser(university, "회장2", "2020000002"));
            persist(ClubMemberFixture.createPresident(club1, president1));
            persist(ClubMemberFixture.createPresident(club2, president2));
            clearPersistenceContext();

            mockLoginUser(normalUser.getId());

            // when & then
            performGet("/clubs?page=1&limit=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubs", hasSize(2)))
                .andExpect(jsonPath("$.totalPage").value(1));
        }

        @Test
        @DisplayName("검색어로 동아리를 필터링한다")
        void getClubsWithQuery() throws Exception {
            // given
            Club club1 = persist(ClubFixture.create(university, "BCSD Lab"));
            Club club2 = persist(ClubFixture.create(university, "스타트업 동아리"));
            User president1 = persist(UserFixture.createUser(university, "회장1", "2020000001"));
            User president2 = persist(UserFixture.createUser(university, "회장2", "2020000002"));
            persist(ClubMemberFixture.createPresident(club1, president1));
            persist(ClubMemberFixture.createPresident(club2, president2));
            clearPersistenceContext();

            mockLoginUser(normalUser.getId());

            // when & then
            performGet("/clubs?page=1&limit=10&query=BCSD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubs", hasSize(1)))
                .andExpect(jsonPath("$.clubs[0].name").value("BCSD Lab"));
        }
    }

    @Nested
    @DisplayName("GET /clubs/{clubId} - 동아리 상세 조회")
    class GetClubDetail {

        @Test
        @DisplayName("동아리 상세 정보를 조회한다")
        void getClubDetailSuccess() throws Exception {
            // given
            Club club = persist(ClubFixture.create(university, "BCSD Lab"));
            User president = persist(UserFixture.createUser(university, "회장", "2020000001"));
            persist(ClubMemberFixture.createPresident(club, president));
            clearPersistenceContext();

            mockLoginUser(normalUser.getId());

            // when & then
            performGet("/clubs/" + club.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("BCSD Lab"))
                .andExpect(jsonPath("$.presidentName").value("회장"))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.isMember").value(false));
        }

        @Test
        @DisplayName("동아리 멤버인 경우 isMember가 true이다")
        void getClubDetailAsMember() throws Exception {
            // given
            Club club = persist(ClubFixture.create(university, "BCSD Lab"));
            persist(ClubMemberFixture.createPresident(club, normalUser));
            clearPersistenceContext();

            mockLoginUser(normalUser.getId());

            // when & then
            performGet("/clubs/" + club.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMember").value(true));
        }

        @Test
        @DisplayName("존재하지 않는 동아리 조회 시 404를 반환한다")
        void getClubDetailNotFound() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            // when & then
            performGet("/clubs/99999")
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /clubs - 동아리 생성")
    class CreateClub {

        @Test
        @DisplayName("어드민이 동아리를 생성한다")
        void createClubByAdminSuccess() throws Exception {
            // given
            User presidentUser = persist(UserFixture.createUser(university, "새회장", "2020000003"));
            clearPersistenceContext();

            mockLoginUser(adminUser.getId());

            ClubCreateRequest request = new ClubCreateRequest(
                presidentUser.getId(),
                "새 동아리",
                "동아리 소개",
                "상세 소개",
                "https://example.com/image.png",
                "학생회관 201호",
                ClubCategory.ACADEMIC
            );

            // when & then
            performPost("/clubs", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("새 동아리"))
                .andExpect(jsonPath("$.presidentName").value("새회장"))
                .andExpect(jsonPath("$.memberCount").value(1));
        }

        @Test
        @DisplayName("일반 유저가 동아리 생성 시 403을 반환한다")
        void createClubByNormalUserFails() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            ClubCreateRequest request = new ClubCreateRequest(
                normalUser.getId(),
                "새 동아리",
                "동아리 소개",
                "상세 소개",
                "https://example.com/image.png",
                "학생회관 201호",
                ClubCategory.ACADEMIC
            );

            // when & then
            performPost("/clubs", request)
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /clubs/joined - 가입한 동아리 조회")
    class GetJoinedClubs {

        @Test
        @DisplayName("사용자가 가입한 동아리 목록을 조회한다")
        void getJoinedClubsSuccess() throws Exception {
            // given
            Club club1 = persist(ClubFixture.create(university, "동아리1"));
            Club club2 = persist(ClubFixture.create(university, "동아리2"));
            persist(ClubMemberFixture.createMember(club1, normalUser));
            persist(ClubMemberFixture.createMember(club2, normalUser));
            clearPersistenceContext();

            mockLoginUser(normalUser.getId());

            // when & then
            performGet("/clubs/joined")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joinedClubs", hasSize(2)));
        }
    }
}
