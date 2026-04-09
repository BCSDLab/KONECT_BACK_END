package gg.agit.konect.integration.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

@DisplayName("회원 탈퇴 API 테스트")
class UserWithdrawApiTest extends IntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    private University university;
    private Club club;

    @BeforeEach
    void setUp() {
        university = persist(UniversityFixture.create());
        club = persist(ClubFixture.create(university, "BCSD Lab"));
    }

    @Nested
    @DisplayName("DELETE /users/withdraw - 회원 탈퇴")
    class Withdraw {

        @Test
        @DisplayName("일반 멤버는 탈퇴할 수 있다")
        void withdrawAsRegularMemberSuccess() throws Exception {
            // given
            User user = persist(UserFixture.createUser(university, "일반회원", "2021136001"));
            persist(ClubMemberFixture.createMember(club, user));
            clearPersistenceContext();

            mockLoginUser(user.getId());

            // when & then
            performDelete("/users/withdraw")
                .andExpect(status().isNoContent());

            // 탈퇴 처리되었는지 확인
            clearPersistenceContext();
            User withdrawnUser = entityManager.find(User.class, user.getId());
            assertThat(withdrawnUser).isNotNull();
            assertThat(withdrawnUser.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("동아리에 가입하지 않은 사용자도 탈퇴할 수 있다")
        void withdrawWithoutClubMembershipSuccess() throws Exception {
            // given
            User user = persist(UserFixture.createUser(university, "미가입자", "2021136002"));
            clearPersistenceContext();

            mockLoginUser(user.getId());

            // when & then
            performDelete("/users/withdraw")
                .andExpect(status().isNoContent());

            clearPersistenceContext();
            User withdrawnUser = entityManager.find(User.class, user.getId());
            assertThat(withdrawnUser).isNotNull();
            assertThat(withdrawnUser.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("회장은 탈퇴할 수 없다")
        void withdrawAsPresidentFails() throws Exception {
            // given
            User president = persist(UserFixture.createUser(university, "회장", "2021136003"));
            persist(ClubMemberFixture.createPresident(club, president));
            clearPersistenceContext();

            mockLoginUser(president.getId());

            // when & then
            performDelete("/users/withdraw")
                .andExpect(status().isBadRequest());

            // 탈퇴 처리되지 않았는지 확인
            clearPersistenceContext();
            User notWithdrawnUser = userRepository.findById(president.getId()).orElse(null);
            assertThat(notWithdrawnUser).isNotNull();
            assertThat(notWithdrawnUser.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("복수 동아리의 회장이면 탈퇴할 수 없다")
        void withdrawAsPresidentOfMultipleClubsFails() throws Exception {
            // given
            Club club2 = persist(ClubFixture.create(university, "Another Club"));
            User president = persist(UserFixture.createUser(university, "다중회장", "2021136004"));

            persist(ClubMemberFixture.createPresident(club, president));
            persist(ClubMemberFixture.createPresident(club2, president));
            clearPersistenceContext();

            mockLoginUser(president.getId());

            // when & then
            performDelete("/users/withdraw")
                .andExpect(status().isBadRequest());

            clearPersistenceContext();
            User notWithdrawnUser = userRepository.findById(president.getId()).orElse(null);
            assertThat(notWithdrawnUser).isNotNull();
            assertThat(notWithdrawnUser.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("한 동아리의 회장이고 다른 동아리의 일반 멤버면 탈퇴할 수 없다")
        void withdrawAsPresidentInOneClubAndMemberInAnotherFails() throws Exception {
            // given
            Club club2 = persist(ClubFixture.create(university, "Another Club"));
            User user = persist(UserFixture.createUser(university, "회장이자일반", "2021136005"));

            persist(ClubMemberFixture.createPresident(club, user));
            persist(ClubMemberFixture.createMember(club2, user));
            clearPersistenceContext();

            mockLoginUser(user.getId());

            // when & then
            performDelete("/users/withdraw")
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("부회장은 탈퇴할 수 있다")
        void withdrawAsVicePresidentSuccess() throws Exception {
            // given
            User vicePresident = persist(UserFixture.createUser(university, "부회장", "2021136006"));
            persist(ClubMemberFixture.createVicePresident(club, vicePresident));
            clearPersistenceContext();

            mockLoginUser(vicePresident.getId());

            // when & then
            performDelete("/users/withdraw")
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("운영진은 탈퇴할 수 있다")
        void withdrawAsManagerSuccess() throws Exception {
            // given
            User manager = persist(UserFixture.createUser(university, "운영진", "2021136007"));
            persist(ClubMemberFixture.createManager(club, manager));
            clearPersistenceContext();

            mockLoginUser(manager.getId());

            // when & then
            performDelete("/users/withdraw")
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("탈퇴 후 재가입을 위해 7일 유예기간이 설정된다")
        void withdrawSetsDeletedAt() throws Exception {
            // given
            User user = persist(UserFixture.createUser(university, "탈퇴자", "2021136008"));
            persist(ClubMemberFixture.createMember(club, user));
            clearPersistenceContext();

            LocalDateTime beforeWithdraw = LocalDateTime.now();
            mockLoginUser(user.getId());

            // when
            performDelete("/users/withdraw")
                .andExpect(status().isNoContent());

            // then
            clearPersistenceContext();
            User withdrawnUser = entityManager.find(User.class, user.getId());
            assertThat(withdrawnUser).isNotNull();
            assertThat(withdrawnUser.getDeletedAt()).isNotNull();
            assertThat(withdrawnUser.getDeletedAt()).isAfterOrEqualTo(beforeWithdraw);
        }

        @Test
        @DisplayName("이미 탈퇴한 사용자가 다시 탈퇴하면 정상 처리된다")
        void doubleWithdrawSucceeds() throws Exception {
            // given
            User user = persist(UserFixture.createUser(university, "이중탈퇴", "2021136009"));
            persist(ClubMemberFixture.createMember(club, user));
            user.withdraw(LocalDateTime.now().minusDays(1));
            persist(user);
            clearPersistenceContext();

            mockLoginUser(user.getId());

            // when & then
            performDelete("/users/withdraw")
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("미인증 사용자는 탈퇴할 수 없다")
        void withdrawWithoutAuthFails() throws Exception {
            // when & then
            performDelete("/users/withdraw")
                .andExpect(status().isNotFound());
        }
    }
}
