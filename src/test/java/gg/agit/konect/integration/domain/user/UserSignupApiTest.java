package gg.agit.konect.integration.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubPreMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.dto.SignupRequest;
import gg.agit.konect.domain.user.model.UnRegisteredUser;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.service.RefreshTokenService;
import gg.agit.konect.domain.user.service.SignupTokenService;
import gg.agit.konect.domain.user.repository.UnRegisteredUserRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.UnRegisteredUserFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import jakarta.servlet.http.Cookie;

@DisplayName("회원가입 API 테스트")
class UserSignupApiTest extends IntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UnRegisteredUserRepository unRegisteredUserRepository;

    @Autowired
    private ClubPreMemberRepository clubPreMemberRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @MockitoBean
    private SignupTokenService signupTokenService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    private static final String SIGNUP_TOKEN_COOKIE_NAME = "signup_token";
    private static final String VALID_SIGNUP_TOKEN = "valid-test-signup-token";
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

    private University university;
    private Club club;
    private User existingPresident;

    @BeforeEach
    void setUp() throws Exception {
        university = persist(UniversityFixture.create());
        club = persist(ClubFixture.create(university, "BCSD Lab"));
        existingPresident = persist(UserFixture.createUser(university, "기존회장", "2020000001"));
        persist(gg.agit.konect.support.fixture.ClubMemberFixture.createPresident(club, existingPresident));
        clearPersistenceContext();
        given(refreshTokenService.issue(anyInt())).willReturn("refresh-token-for-test");
        given(refreshTokenService.refreshTtl()).willReturn(REFRESH_TOKEN_TTL);
        given(jwtProvider.createToken(anyInt())).willReturn("access-token-for-test");
    }

    @Nested
    @DisplayName("POST /users/signup - 회원가입")
    class Signup {

        @Test
        @DisplayName("정상 회원가입을 성공한다")
        void signupSuccess() throws Exception {
            // given
            String email = "newuser@koreatech.ac.kr";
            String studentNumber = "2021136001";
            UnRegisteredUser unRegisteredUser = UnRegisteredUserFixture.createGoogle(email);
            persist(unRegisteredUser);
            clearPersistenceContext();

            SignupRequest request = new SignupRequest(
                "홍길동",
                university.getId(),
                studentNumber,
                true
            );
            stubSignupTokenClaims(email);

            // when & then
            performSignup(request)
                .andExpect(status().isOk());

            // 회원이 생성되었는지 확인
            clearPersistenceContext();
            User savedUser = findSavedUser(studentNumber);
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getName()).isEqualTo("홍길동");
            assertThat(savedUser.getEmail()).isEqualTo(email);
        }

        @Test
        @DisplayName("회원가입 시 PreMember가 있으면 자동으로 동아리에 가입된다")
        void signupWithPreMemberAutoJoinsClub() throws Exception {
            // given
            String email = "premember@koreatech.ac.kr";
            String studentNumber = "2021136002";
            String name = "김프리";

            UnRegisteredUser unRegisteredUser = UnRegisteredUserFixture.createGoogle(email);
            persist(unRegisteredUser);

            // PreMember로 등록 (MEMBER 직급)
            ClubPreMember preMember = ClubPreMember.builder()
                .club(club)
                .studentNumber(studentNumber)
                .name(name)
                .clubPosition(ClubPosition.MEMBER)
                .build();
            persist(preMember);
            clearPersistenceContext();

            SignupRequest request = new SignupRequest(name, university.getId(), studentNumber, true);
            stubSignupTokenClaims(email);

            // when
            performSignup(request)
                .andExpect(status().isOk());

            // then
            clearPersistenceContext();
            User savedUser = findSavedUser(studentNumber);
            assertThat(savedUser).isNotNull();

            // 동아리 멤버로 등록되었는지 확인
            boolean isMember = clubMemberRepository.existsByClubIdAndUserId(club.getId(), savedUser.getId());
            assertThat(isMember).isTrue();

            ClubMember clubMember = clubMemberRepository.getByClubIdAndUserId(club.getId(), savedUser.getId());
            assertThat(clubMember.getClubPosition()).isEqualTo(ClubPosition.MEMBER);

            // PreMember는 삭제되었는지 확인
            List<ClubPreMember> remainingPreMembers = clubPreMemberRepository.findAllByClubId(club.getId());
            assertThat(remainingPreMembers).isEmpty();
        }

        @Test
        @DisplayName("회원가입 시 PreMember가 회장이면 기존 회장을 교체한다")
        void signupWithPreMemberPresidentReplacesExistingPresident() throws Exception {
            // given
            String email = "newpresident@koreatech.ac.kr";
            String studentNumber = "2021136003";
            String name = "신임회장";

            UnRegisteredUser unRegisteredUser = UnRegisteredUserFixture.createGoogle(email);
            persist(unRegisteredUser);

            // PreMember로 회장 등록
            ClubPreMember preMemberPresident = ClubPreMember.builder()
                .club(club)
                .studentNumber(studentNumber)
                .name(name)
                .clubPosition(ClubPosition.PRESIDENT)
                .build();
            persist(preMemberPresident);
            clearPersistenceContext();

            // 기존 회장이 존재하는지 확인
            assertThat(clubMemberRepository.findPresidentByClubId(club.getId())).isPresent();

            SignupRequest request = new SignupRequest(name, university.getId(), studentNumber, true);
            stubSignupTokenClaims(email);

            // when
            performSignup(request)
                .andExpect(status().isOk());

            // then
            clearPersistenceContext();
            User savedUser = findSavedUser(studentNumber);
            assertThat(savedUser).isNotNull();

            // 새로운 사용자가 회장으로 등록되었는지 확인
            ClubMember newPresident = clubMemberRepository.getByClubIdAndUserId(club.getId(), savedUser.getId());
            assertThat(newPresident.getClubPosition()).isEqualTo(ClubPosition.PRESIDENT);

            // 기존 회장은 삭제되었는지 확인
            assertThat(clubMemberRepository.findPresidentByClubId(club.getId())).isPresent();
            assertThat(clubMemberRepository.findPresidentByClubId(club.getId()).get().getUser().getId())
                .isEqualTo(savedUser.getId());
        }

        @Test
        @DisplayName("회원가입 시 복수 동아리의 PreMember가 있으면 모두 가입된다")
        void signupWithMultiplePreMembersJoinsAllClubs() throws Exception {
            // given
            String email = "multi@koreatech.ac.kr";
            String studentNumber = "2021136004";
            String name = "멀티동아리";

            Club club2 = persist(ClubFixture.create(university, "Another Club"));

            UnRegisteredUser unRegisteredUser = UnRegisteredUserFixture.createGoogle(email);
            persist(unRegisteredUser);

            // 두 동아리에 PreMember 등록
            ClubPreMember preMember1 = ClubPreMember.builder()
                .club(club)
                .studentNumber(studentNumber)
                .name(name)
                .clubPosition(ClubPosition.MEMBER)
                .build();
            ClubPreMember preMember2 = ClubPreMember.builder()
                .club(club2)
                .studentNumber(studentNumber)
                .name(name)
                .clubPosition(ClubPosition.MANAGER)
                .build();
            persist(preMember1);
            persist(preMember2);
            clearPersistenceContext();

            SignupRequest request = new SignupRequest(name, university.getId(), studentNumber, true);
            stubSignupTokenClaims(email);

            // when
            performSignup(request)
                .andExpect(status().isOk());

            // then
            clearPersistenceContext();
            User savedUser = findSavedUser(studentNumber);
            assertThat(savedUser).isNotNull();

            // 두 동아리 모두 가입되었는지 확인
            boolean isMemberOfClub1 = clubMemberRepository.existsByClubIdAndUserId(club.getId(), savedUser.getId());
            boolean isMemberOfClub2 = clubMemberRepository.existsByClubIdAndUserId(club2.getId(), savedUser.getId());
            assertThat(isMemberOfClub1).isTrue();
            assertThat(isMemberOfClub2).isTrue();

            // 각각의 직급 확인
            ClubMember memberInClub1 = clubMemberRepository.getByClubIdAndUserId(club.getId(), savedUser.getId());
            ClubMember memberInClub2 = clubMemberRepository.getByClubIdAndUserId(club2.getId(), savedUser.getId());
            assertThat(memberInClub1.getClubPosition()).isEqualTo(ClubPosition.MEMBER);
            assertThat(memberInClub2.getClubPosition()).isEqualTo(ClubPosition.MANAGER);
        }

        @Test
        @DisplayName("회원가입 시 이름이 유효하지 않으면 400을 반환한다")
        void signupWithInvalidNameReturns400() throws Exception {
            // given
            String email = "test@koreatech.ac.kr";
            UnRegisteredUser unRegisteredUser = UnRegisteredUserFixture.createGoogle(email);
            persist(unRegisteredUser);
            clearPersistenceContext();

            // 이름 1글자 (유효하지 않음)
            SignupRequest request = new SignupRequest("홍", university.getId(), "2021136005", true);
            stubSignupTokenClaims(email);

            // when & then
            performSignup(request)
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("회원가입 시 학번이 숫자가 아니면 400을 반환한다")
        void signupWithNonNumericStudentNumberReturns400() throws Exception {
            // given
            String email = "test@koreatech.ac.kr";
            UnRegisteredUser unRegisteredUser = UnRegisteredUserFixture.createGoogle(email);
            persist(unRegisteredUser);
            clearPersistenceContext();

            SignupRequest request = new SignupRequest("홍길동", university.getId(), "ABC123", true);
            stubSignupTokenClaims(email);

            // when & then
            performSignup(request)
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("회원가입 시 존재하지 않는 대학 ID면 404를 반환한다")
        void signupWithNonExistentUniversityReturns404() throws Exception {
            // given
            String email = "test@koreatech.ac.kr";
            UnRegisteredUser unRegisteredUser = UnRegisteredUserFixture.createGoogle(email);
            persist(unRegisteredUser);
            clearPersistenceContext();

            SignupRequest request = new SignupRequest("홍길동", 99999, "2021136006", true);
            stubSignupTokenClaims(email);

            // when & then
            performSignup(request)
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("회원가입 시 마케팅 동의 여부가 null이면 400을 반환한다")
        void signupWithNullMarketingAgreementReturns400() throws Exception {
            // given
            String email = "test@koreatech.ac.kr";
            UnRegisteredUser unRegisteredUser = UnRegisteredUserFixture.createGoogle(email);
            persist(unRegisteredUser);
            clearPersistenceContext();

            // 마케팅 동의 null인 DTO 생성
            String jsonRequest = String.format(
                "{\"name\": \"홍길동\", \"universityId\": %d, \"studentNumber\": \"2021136007\"}",
                university.getId()
            );
            stubSignupTokenClaims(email);

            // when & then
            mockMvc.perform(post("/users/signup")
                    .cookie(signupTokenCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PreMember가 없는 이름/학번 조합이면 자동 가입되지 않는다")
        void signupWithoutMatchingPreMemberDoesNotAutoJoin() throws Exception {
            // given
            String email = "nomatch@koreatech.ac.kr";
            String studentNumber = "2021136008";
            String name = "노매치";

            UnRegisteredUser unRegisteredUser = UnRegisteredUserFixture.createGoogle(email);
            persist(unRegisteredUser);

            // 다른 학번으로 PreMember 등록
            ClubPreMember preMember = ClubPreMember.builder()
                .club(club)
                .studentNumber("99999999")  // 다른 학번
                .name(name)
                .clubPosition(ClubPosition.MEMBER)
                .build();
            persist(preMember);
            clearPersistenceContext();

            SignupRequest request = new SignupRequest(name, university.getId(), studentNumber, true);
            stubSignupTokenClaims(email);

            // when
            performSignup(request)
                .andExpect(status().isOk());

            // then
            clearPersistenceContext();
            User savedUser = findSavedUser(studentNumber);
            assertThat(savedUser).isNotNull();

            // 동아리에 가입되지 않았는지 확인
            boolean isMember = clubMemberRepository.existsByClubIdAndUserId(club.getId(), savedUser.getId());
            assertThat(isMember).isFalse();
        }
    }

    private User findSavedUser(String studentNumber) {
        return userRepository.findAllByUniversityIdAndStudentNumber(
                university.getId(),
                studentNumber
            ).stream()
            .findFirst()
            .orElse(null);
    }

    private void stubSignupTokenClaims(String email) {
        String providerId = "google_" + email.split("@")[0];
        given(signupTokenService.consumeOrThrow(VALID_SIGNUP_TOKEN))
            .willReturn(new SignupTokenService.SignupClaims(email, Provider.GOOGLE, providerId, "임시유저"));
    }

    private ResultActions performSignup(SignupRequest request) throws Exception {
        return mockMvc.perform(post("/users/signup")
            .cookie(signupTokenCookie())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
    }

    private Cookie signupTokenCookie() {
        return new Cookie(SIGNUP_TOKEN_COOKIE_NAME, VALID_SIGNUP_TOKEN);
    }
}
