package gg.agit.konect.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.dto.SignupRequest;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.UnRegisteredUser;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.UnRegisteredUserFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

@Transactional
class UserServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    private University university;

    @BeforeEach
    void setUp() {
        university = persist(UniversityFixture.create());
    }

    @Nested
    @DisplayName("회원가입")
    class Signup {

        @Test
        @DisplayName("새로운 사용자가 회원가입한다")
        void signupSuccess() {
            // given
            String email = "newuser@koreatech.ac.kr";
            UnRegisteredUser unRegisteredUser = persist(UnRegisteredUserFixture.createGoogle(email));
            clearPersistenceContext();

            SignupRequest request = new SignupRequest(
                "홍길동",
                university.getId(),
                "2025000001",
                true
            );

            // when
            Integer userId = userService.signup(
                email,
                unRegisteredUser.getProviderId(),
                Provider.GOOGLE,
                request
            );

            // then
            assertThat(userId).isNotNull();
            User savedUser = userRepository.getById(userId);
            assertThat(savedUser.getName()).isEqualTo("홍길동");
            assertThat(savedUser.getStudentNumber()).isEqualTo("2025000001");
            assertThat(savedUser.getUniversity().getId()).isEqualTo(university.getId());
        }

        @Test
        @DisplayName("이미 등록된 사용자가 회원가입 시도하면 예외가 발생한다")
        void signupAlreadyRegisteredUserFails() {
            // given
            String email = "existing@koreatech.ac.kr";
            User existingUser = persist(UserFixture.createUser(university, "기존유저", "2020000001"));
            UnRegisteredUser unRegisteredUser = persist(UnRegisteredUserFixture.createGoogle(email));
            clearPersistenceContext();

            SignupRequest request = new SignupRequest(
                "새유저",
                university.getId(),
                "2025000002",
                true
            );

            // when & then
            assertThatThrownBy(() -> userService.signup(
                existingUser.getEmail(),
                existingUser.getProviderId(),
                existingUser.getProvider(),
                request
            )).isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("중복된 학번으로 회원가입 시도하면 예외가 발생한다")
        void signupDuplicateStudentNumberFails() {
            // given
            String existingStudentNumber = "2020000001";
            persist(UserFixture.createUser(university, "기존유저", existingStudentNumber));

            String newEmail = "newuser@koreatech.ac.kr";
            UnRegisteredUser unRegisteredUser = persist(UnRegisteredUserFixture.createGoogle(newEmail));
            clearPersistenceContext();

            SignupRequest request = new SignupRequest(
                "새유저",
                university.getId(),
                existingStudentNumber,
                true
            );

            // when & then
            assertThatThrownBy(() -> userService.signup(
                newEmail,
                unRegisteredUser.getProviderId(),
                Provider.GOOGLE,
                request
            )).isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    class DeleteUser {

        @Test
        @DisplayName("동아리 회장인 경우 탈퇴할 수 없다")
        void deleteUserAsPresidentFails() {
            // given
            User president = persist(UserFixture.createUser(university, "회장", "2021136001"));
            Club club = persist(ClubFixture.create(university, "동아리"));
            persist(ClubMemberFixture.createPresident(club, president));
            clearPersistenceContext();

            // when & then
            assertThatThrownBy(() -> userService.deleteUser(president.getId()))
                .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("동아리에 가입하지 않은 사용자는 탈퇴할 수 있다")
        void deleteUserWithNoClubSuccess() {
            // given
            User user = persist(UserFixture.createUser(university, "일반유저", "2021136001"));
            Integer userId = user.getId();
            clearPersistenceContext();

            // when
            userService.deleteUser(userId);
            clearPersistenceContext();

            // then - 삭제된 사용자는 조회되지 않음 (soft delete)
            assertThatThrownBy(() -> userRepository.getById(userId))
                .isInstanceOf(CustomException.class);
        }
    }
}
