package gg.agit.konect.integration.domain.user;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class UserApiTest extends IntegrationTestSupport {

    private University university;

    @BeforeEach
    void setUp() throws Exception {
        university = persist(UniversityFixture.create());
    }

    @Nested
    @DisplayName("DELETE /users/withdraw - 회원 탈퇴")
    class Withdraw {

        @Test
        @DisplayName("동아리 회장인 경우 탈퇴할 수 없다")
        void withdrawAsPresidentFails() throws Exception {
            // given
            User president = persist(UserFixture.createUser(university, "회장", "2020000001"));
            Club club = persist(ClubFixture.create(university, "동아리"));
            persist(ClubMemberFixture.createPresident(club, president));
            clearPersistenceContext();

            mockLoginUser(president.getId());

            // when & then
            performDelete("/users/withdraw")
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("동아리에 가입하지 않은 사용자는 탈퇴할 수 있다")
        void withdrawWithNoClubSuccess() throws Exception {
            // given
            User noClubUser = persist(UserFixture.createUser(university, "무소속유저", "2022000001"));
            clearPersistenceContext();

            mockLoginUser(noClubUser.getId());

            // when & then
            performDelete("/users/withdraw")
                .andExpect(status().isNoContent());
        }
    }
}
