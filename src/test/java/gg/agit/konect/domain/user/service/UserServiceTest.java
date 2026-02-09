package gg.agit.konect.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.notice.repository.CouncilNoticeReadRepository;
import gg.agit.konect.domain.studytime.service.StudyTimeQueryService;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.university.repository.UniversityRepository;
import gg.agit.konect.domain.user.dto.SignupRequest;
import gg.agit.konect.domain.user.dto.UserInfoResponse;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.event.UserRegisteredEvent;
import gg.agit.konect.domain.user.event.UserWithdrawnEvent;
import gg.agit.konect.domain.user.model.UnRegisteredUser;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UnRegisteredUserRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    private static final int USER_ID = 10;
    private static final int ADMIN_ID = 1;
    private static final int UNIVERSITY_ID = 1;
    private static final String EMAIL = "user@example.com";
    private static final String PROVIDER_ID = "provider-id";
    private static final String STUDENT_NUMBER = "20250001";
    private static final String INVALID_REQUEST_BODY_MESSAGE = "잘못된 입력값이 포함되어 있습니다.";
    private static final String ALREADY_REGISTERED_USER_MESSAGE = "이미 가입된 회원입니다.";
    private static final String UNIVERSITY_NOT_FOUND_MESSAGE = "대학교를 찾을 수 없습니다.";
    private static final String DUPLICATE_STUDENT_NUMBER_MESSAGE = "이미 사용 중인 학번입니다.";
    private static final String CANNOT_DELETE_CLUB_PRESIDENT_MESSAGE = "동아리 회장인 경우 회장을 양도하고 탈퇴해야 합니다.";
    private static final String CANNOT_DELETE_USER_WITH_UNPAID_FEE_MESSAGE = "미납 회비가 있는 경우 탈퇴할 수 없습니다.";

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UnRegisteredUserRepository unRegisteredUserRepository;

    @Mock
    private UniversityRepository universityRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private ClubPreMemberRepository clubPreMemberRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private CouncilNoticeReadRepository councilNoticeReadRepository;

    @Mock
    private StudyTimeQueryService studyTimeQueryService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Nested
    @DisplayName("signup 테스트")
    class SignupTests {

        @Test
        @DisplayName("APPLE 제공자에서 providerId가 비어있으면 예외를 던진다")
        void signupAppleWithoutProviderIdThrowsException() {
            // Given
            SignupRequest request = createSignupRequest();

            // When & Then
            assertThatThrownBy(() -> userService.signup(EMAIL, "", Provider.APPLE, request))
                .isInstanceOf(CustomException.class)
                .hasMessage(INVALID_REQUEST_BODY_MESSAGE);
        }

        @Test
        @DisplayName("providerId가 이미 존재하면 예외를 던진다")
        void signupWithDuplicatedProviderIdThrowsException() {
            // Given
            SignupRequest request = createSignupRequest();
            given(userRepository.findByProviderIdAndProvider(PROVIDER_ID, Provider.GOOGLE))
                .willReturn(Optional.of(mock(User.class)));

            // When & Then
            assertThatThrownBy(() -> userService.signup(EMAIL, PROVIDER_ID, Provider.GOOGLE, request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ALREADY_REGISTERED_USER_MESSAGE);
        }

        @Test
        @DisplayName("이메일이 이미 존재하면 예외를 던진다")
        void signupWithDuplicatedEmailThrowsException() {
            // Given
            SignupRequest request = createSignupRequest();
            given(userRepository.findByEmailAndProvider(EMAIL, Provider.GOOGLE))
                .willReturn(Optional.of(mock(User.class)));

            // When & Then
            assertThatThrownBy(() -> userService.signup(EMAIL, null, Provider.GOOGLE, request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ALREADY_REGISTERED_USER_MESSAGE);
        }

        @Test
        @DisplayName("providerId로 임시유저를 조회할 수 있으면 해당 경로로 회원가입을 진행한다")
        void signupWithProviderIdResolvesTempUserByProviderPath() {
            // Given
            SignupRequest request = createSignupRequest();
            University university = createUniversity();
            UnRegisteredUser tempUser = createTempUser();
            User savedUser = createUser(USER_ID, EMAIL, STUDENT_NUMBER, university);

            given(userRepository.findByProviderIdAndProvider(PROVIDER_ID, Provider.GOOGLE)).willReturn(Optional.empty());
            given(userRepository.findByEmailAndProvider(EMAIL, Provider.GOOGLE)).willReturn(Optional.empty());
            given(unRegisteredUserRepository.existsByProviderIdAndProvider(PROVIDER_ID, Provider.GOOGLE)).willReturn(true);
            given(unRegisteredUserRepository.getByProviderIdAndProvider(PROVIDER_ID, Provider.GOOGLE)).willReturn(tempUser);
            given(universityRepository.findById(UNIVERSITY_ID)).willReturn(Optional.of(university));
            given(userRepository.existsByUniversityIdAndStudentNumber(UNIVERSITY_ID, STUDENT_NUMBER)).willReturn(false);
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(clubPreMemberRepository.findAllByUniversityIdAndStudentNumberAndName(
                UNIVERSITY_ID,
                STUDENT_NUMBER,
                request.name()
            )).willReturn(List.of());
            given(userRepository.findFirstByRoleOrderByIdAsc(UserRole.ADMIN)).willReturn(Optional.empty());

            // When
            Integer result = userService.signup(EMAIL, PROVIDER_ID, Provider.GOOGLE, request);

            // Then
            assertThat(result).isEqualTo(USER_ID);
            verify(unRegisteredUserRepository).getByProviderIdAndProvider(PROVIDER_ID, Provider.GOOGLE);
        }

        @Test
        @DisplayName("대학교를 찾지 못하면 예외를 던진다")
        void signupWithUnknownUniversityThrowsException() {
            // Given
            SignupRequest request = createSignupRequest();
            given(userRepository.findByEmailAndProvider(EMAIL, Provider.GOOGLE)).willReturn(Optional.empty());
            given(unRegisteredUserRepository.getByEmailAndProvider(EMAIL, Provider.GOOGLE)).willReturn(createTempUser());
            given(universityRepository.findById(UNIVERSITY_ID)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.signup(EMAIL, null, Provider.GOOGLE, request))
                .isInstanceOf(CustomException.class)
                .hasMessage(UNIVERSITY_NOT_FOUND_MESSAGE);
        }

        @Test
        @DisplayName("학번 중복이면 예외를 던진다")
        void signupWithDuplicatedStudentNumberThrowsException() {
            // Given
            SignupRequest request = createSignupRequest();
            University university = createUniversity();

            given(userRepository.findByEmailAndProvider(EMAIL, Provider.GOOGLE)).willReturn(Optional.empty());
            given(unRegisteredUserRepository.getByEmailAndProvider(EMAIL, Provider.GOOGLE)).willReturn(createTempUser());
            given(universityRepository.findById(UNIVERSITY_ID)).willReturn(Optional.of(university));
            given(userRepository.existsByUniversityIdAndStudentNumber(UNIVERSITY_ID, STUDENT_NUMBER)).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.signup(EMAIL, null, Provider.GOOGLE, request))
                .isInstanceOf(CustomException.class)
                .hasMessage(DUPLICATE_STUDENT_NUMBER_MESSAGE);
        }

        @Test
        @DisplayName("회원가입 성공 시 사용자 저장과 이벤트 발행을 수행한다")
        void signupSuccessSavesUserAndPublishesEvent() {
            // Given
            SignupRequest request = createSignupRequest();
            University university = createUniversity();
            UnRegisteredUser tempUser = createTempUser();
            User savedUser = createUser(USER_ID, EMAIL, STUDENT_NUMBER, university);

            given(userRepository.findByEmailAndProvider(EMAIL, Provider.GOOGLE)).willReturn(Optional.empty());
            given(unRegisteredUserRepository.getByEmailAndProvider(EMAIL, Provider.GOOGLE)).willReturn(tempUser);
            given(universityRepository.findById(UNIVERSITY_ID)).willReturn(Optional.of(university));
            given(userRepository.existsByUniversityIdAndStudentNumber(UNIVERSITY_ID, STUDENT_NUMBER)).willReturn(false);
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(clubPreMemberRepository.findAllByUniversityIdAndStudentNumberAndName(
                UNIVERSITY_ID,
                STUDENT_NUMBER,
                request.name()
            )).willReturn(List.of());
            given(userRepository.findFirstByRoleOrderByIdAsc(UserRole.ADMIN)).willReturn(Optional.empty());

            // When
            Integer result = userService.signup(EMAIL, null, Provider.GOOGLE, request);

            // Then
            assertThat(result).isEqualTo(USER_ID);
            verify(unRegisteredUserRepository).delete(tempUser);
            verify(applicationEventPublisher).publishEvent(UserRegisteredEvent.from(EMAIL));
            verifyNoInteractions(chatRoomRepository, chatMessageRepository);
        }

        @Test
        @DisplayName("가입 대기 멤버가 있으면 동아리 멤버로 전환하고 preMember를 삭제한다")
        void signupWithPreMembersSavesClubMembersAndDeletesPreMembers() {
            // Given
            SignupRequest request = createSignupRequest();
            University university = createUniversity();
            UnRegisteredUser tempUser = createTempUser();
            User savedUser = createUser(USER_ID, EMAIL, STUDENT_NUMBER, university);
            gg.agit.konect.domain.club.model.ClubPreMember preMember = createPreMember();
            given(userRepository.findByEmailAndProvider(EMAIL, Provider.GOOGLE)).willReturn(Optional.empty());
            given(unRegisteredUserRepository.getByEmailAndProvider(EMAIL, Provider.GOOGLE)).willReturn(tempUser);
            given(universityRepository.findById(UNIVERSITY_ID)).willReturn(Optional.of(university));
            given(userRepository.existsByUniversityIdAndStudentNumber(UNIVERSITY_ID, STUDENT_NUMBER)).willReturn(false);
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(clubPreMemberRepository.findAllByUniversityIdAndStudentNumberAndName(
                UNIVERSITY_ID,
                STUDENT_NUMBER,
                request.name()
            )).willReturn(List.of(preMember));
            given(userRepository.findFirstByRoleOrderByIdAsc(UserRole.ADMIN)).willReturn(Optional.empty());

            // When
            userService.signup(EMAIL, null, Provider.GOOGLE, request);

            // Then
            verify(clubMemberRepository).save(any(ClubMember.class));
            verify(clubPreMemberRepository).deleteAll(List.of(preMember));
        }

        @Test
        @DisplayName("운영자와 채팅방이 있으면 환영 메시지를 전송한다")
        void signupWithAdminSendsWelcomeMessage() {
            // Given
            SignupRequest request = createSignupRequest();
            University university = createUniversity();
            UnRegisteredUser tempUser = createTempUser();
            User savedUser = createUser(USER_ID, EMAIL, STUDENT_NUMBER, university);
            User admin = createUser(ADMIN_ID, "admin@example.com", "20200001", university);
            gg.agit.konect.domain.chat.model.ChatRoom chatRoom = mock(gg.agit.konect.domain.chat.model.ChatRoom.class);
            gg.agit.konect.domain.chat.model.ChatMessage chatMessage = createChatMessage();
            given(userRepository.findByEmailAndProvider(EMAIL, Provider.GOOGLE)).willReturn(Optional.empty());
            given(unRegisteredUserRepository.getByEmailAndProvider(EMAIL, Provider.GOOGLE)).willReturn(tempUser);
            given(universityRepository.findById(UNIVERSITY_ID)).willReturn(Optional.of(university));
            given(userRepository.existsByUniversityIdAndStudentNumber(UNIVERSITY_ID, STUDENT_NUMBER)).willReturn(false);
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(clubPreMemberRepository.findAllByUniversityIdAndStudentNumberAndName(
                UNIVERSITY_ID,
                STUDENT_NUMBER,
                request.name()
            )).willReturn(List.of());
            given(userRepository.findFirstByRoleOrderByIdAsc(UserRole.ADMIN)).willReturn(Optional.of(admin));
            given(chatRoomRepository.findByTwoUsers(ADMIN_ID, USER_ID)).willReturn(Optional.of(chatRoom));
            given(chatMessageRepository.save(any(gg.agit.konect.domain.chat.model.ChatMessage.class))).willReturn(chatMessage);

            // When
            userService.signup(EMAIL, null, Provider.GOOGLE, request);

            // Then
            verify(chatMessageRepository).save(any(gg.agit.konect.domain.chat.model.ChatMessage.class));
            verify(chatRoom).updateLastMessage(any(String.class), any(java.time.LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("deleteUser 테스트")
    class DeleteUserTests {

        @Test
        @DisplayName("동아리 회장이면 탈퇴할 수 없다")
        void deleteUserPresidentThrowsException() {
            // Given
            User user = createUser(USER_ID, EMAIL, STUDENT_NUMBER, createUniversity());
            ClubMember presidentMember = mock(ClubMember.class);

            given(userRepository.getById(USER_ID)).willReturn(user);
            given(clubMemberRepository.findByUserId(USER_ID)).willReturn(List.of(presidentMember));
            given(presidentMember.isPresident()).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.deleteUser(USER_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(CANNOT_DELETE_CLUB_PRESIDENT_MESSAGE);
        }

        @Test
        @DisplayName("미납 회비가 있으면 탈퇴할 수 없다")
        void deleteUserWithUnpaidFeeThrowsException() {
            // Given
            User user = createUser(USER_ID, EMAIL, STUDENT_NUMBER, createUniversity());
            ClubMember normalMember = mock(ClubMember.class);
            ClubMember unpaidMember = mock(ClubMember.class);

            given(userRepository.getById(USER_ID)).willReturn(user);
            given(clubMemberRepository.findByUserId(USER_ID))
                .willReturn(List.of(normalMember))
                .willReturn(List.of(unpaidMember));
            given(normalMember.isPresident()).willReturn(false);
            given(unpaidMember.hasUnpaidFee()).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.deleteUser(USER_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(CANNOT_DELETE_USER_WITH_UNPAID_FEE_MESSAGE);
        }

        @Test
        @DisplayName("유효한 상태이면 탈퇴 처리와 이벤트 발행을 수행한다")
        void deleteUserSuccessDeletesUserAndPublishesEvent() {
            // Given
            User user = createUser(USER_ID, EMAIL, STUDENT_NUMBER, createUniversity());
            ClubMember nonPresidentMember = mock(ClubMember.class);
            ClubMember paidMember = mock(ClubMember.class);

            given(userRepository.getById(USER_ID)).willReturn(user);
            given(clubMemberRepository.findByUserId(USER_ID))
                .willReturn(List.of(nonPresidentMember))
                .willReturn(List.of(paidMember));
            given(nonPresidentMember.isPresident()).willReturn(false);
            given(paidMember.hasUnpaidFee()).willReturn(false);

            // When
            userService.deleteUser(USER_ID);

            // Then
            verify(userRepository).delete(user);
            verify(applicationEventPublisher).publishEvent(UserWithdrawnEvent.from(EMAIL));
        }

        @Test
        @DisplayName("클럽 활동이 없어도 정상 탈퇴한다")
        void deleteUserWithoutClubMembersSucceeds() {
            // Given
            User user = createUser(USER_ID, EMAIL, STUDENT_NUMBER, createUniversity());
            given(userRepository.getById(USER_ID)).willReturn(user);
            given(clubMemberRepository.findByUserId(USER_ID)).willReturn(List.of(), List.of());

            // When
            userService.deleteUser(USER_ID);

            // Then
            verify(userRepository).delete(user);
            verify(applicationEventPublisher).publishEvent(UserWithdrawnEvent.from(EMAIL));
        }
    }

    @Nested
    @DisplayName("getUserInfo 테스트")
    class GetUserInfoTests {

        @Test
        @DisplayName("유저 정보와 집계 정보를 조합해 응답을 반환한다")
        void getUserInfoCombinesAggregatedFields() {
            // Given
            University university = createUniversity();
            User user = createUser(USER_ID, EMAIL, STUDENT_NUMBER, university);

            ClubMember presidentMember = mock(ClubMember.class);
            ClubMember member = mock(ClubMember.class);

            given(userRepository.getById(USER_ID)).willReturn(user);
            given(clubMemberRepository.findAllByUserId(USER_ID)).willReturn(List.of(presidentMember, member));
            given(presidentMember.isPresident()).willReturn(true);
            given(councilNoticeReadRepository.countUnreadNoticesByUserId(USER_ID)).willReturn(3L);
            given(studyTimeQueryService.getTotalStudyTime(USER_ID)).willReturn(3720L);

            // When
            UserInfoResponse response = userService.getUserInfo(USER_ID);

            // Then
            assertThat(response.name()).isEqualTo("홍길동");
            assertThat(response.universityName()).isEqualTo("한국기술교육대학교");
            assertThat(response.joinedClubCount()).isEqualTo(2);
            assertThat(response.unreadCouncilNoticeCount()).isEqualTo(3L);
            assertThat(response.studyTime()).isEqualTo("01:02");
            assertThat(response.isClubManager()).isTrue();
        }

        @Test
        @DisplayName("회장이 없으면 isClubManager는 false다")
        void getUserInfoWithoutPresidentReturnsFalseManager() {
            // Given
            University university = createUniversity();
            User user = createUser(USER_ID, EMAIL, STUDENT_NUMBER, university);
            ClubMember member = mock(ClubMember.class);

            given(userRepository.getById(USER_ID)).willReturn(user);
            given(clubMemberRepository.findAllByUserId(USER_ID)).willReturn(List.of(member));
            given(member.isPresident()).willReturn(false);
            given(councilNoticeReadRepository.countUnreadNoticesByUserId(USER_ID)).willReturn(0L);
            given(studyTimeQueryService.getTotalStudyTime(USER_ID)).willReturn(0L);

            // When
            UserInfoResponse response = userService.getUserInfo(USER_ID);

            // Then
            assertThat(response.joinedClubCount()).isEqualTo(1);
            assertThat(response.isClubManager()).isFalse();
            assertThat(response.studyTime()).isEqualTo("00:00");
        }
    }

    private SignupRequest createSignupRequest() {
        return new SignupRequest("홍길동", UNIVERSITY_ID, STUDENT_NUMBER, true);
    }

    private UnRegisteredUser createTempUser() {
        UnRegisteredUser user = newInstance(UnRegisteredUser.class);
        ReflectionTestUtils.setField(user, "id", 5);
        ReflectionTestUtils.setField(user, "email", EMAIL);
        ReflectionTestUtils.setField(user, "provider", Provider.GOOGLE);
        ReflectionTestUtils.setField(user, "providerId", null);
        return user;
    }

    private University createUniversity() {
        University university = newInstance(University.class);
        ReflectionTestUtils.setField(university, "id", UNIVERSITY_ID);
        ReflectionTestUtils.setField(university, "koreanName", "한국기술교육대학교");
        return university;
    }

    private User createUser(int id, String email, String studentNumber, University university) {
        User user = newInstance(User.class);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "email", email);
        ReflectionTestUtils.setField(user, "name", "홍길동");
        ReflectionTestUtils.setField(user, "studentNumber", studentNumber);
        ReflectionTestUtils.setField(user, "university", university);
        ReflectionTestUtils.setField(user, "provider", Provider.GOOGLE);
        ReflectionTestUtils.setField(user, "providerId", PROVIDER_ID);
        ReflectionTestUtils.setField(user, "imageUrl", "https://stage-static.koreatech.in/konect/User_02.png");
        ReflectionTestUtils.setField(user, "phoneNumber", null);
        ReflectionTestUtils.setField(user, "isMarketingAgreement", true);
        return user;
    }

    private gg.agit.konect.domain.club.model.ClubPreMember createPreMember() {
        gg.agit.konect.domain.club.model.ClubPreMember preMember = newInstance(gg.agit.konect.domain.club.model.ClubPreMember.class);
        ReflectionTestUtils.setField(preMember, "id", 77);
        ReflectionTestUtils.setField(preMember, "club", mock(gg.agit.konect.domain.club.model.Club.class));
        ReflectionTestUtils.setField(preMember, "studentNumber", STUDENT_NUMBER);
        ReflectionTestUtils.setField(preMember, "name", "홍길동");
        return preMember;
    }

    private gg.agit.konect.domain.chat.model.ChatMessage createChatMessage() {
        gg.agit.konect.domain.chat.model.ChatMessage message = newInstance(gg.agit.konect.domain.chat.model.ChatMessage.class);
        ReflectionTestUtils.setField(message, "id", 101);
        ReflectionTestUtils.setField(message, "content", "welcome");
        ReflectionTestUtils.setField(message, "createdAt", java.time.LocalDateTime.now());
        return message;
    }

    private <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("객체 생성 실패: " + type.getSimpleName(), e);
        }
    }
}
