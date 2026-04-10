package gg.agit.konect.domain.club.service;

import static gg.agit.konect.global.code.ApiResponseCode.AMBIGUOUS_USER_MATCH;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_CHANGE_OWN_POSITION;
import static gg.agit.konect.global.code.ApiResponseCode.MANAGER_LIMIT_EXCEEDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import gg.agit.konect.domain.chat.service.ChatRoomMembershipService;
import gg.agit.konect.domain.club.dto.ClubPreMemberAddRequest;
import gg.agit.konect.domain.club.dto.ClubPreMemberAddResponse;
import gg.agit.konect.domain.club.dto.MemberPositionChangeRequest;
import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.UniversityFixture;

class ClubMemberManagementServiceTest extends ServiceTestSupport {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private ClubPreMemberRepository clubPreMemberRepository;

    @Mock
    private ClubPermissionValidator clubPermissionValidator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomMembershipService chatRoomMembershipService;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private ClubMemberManagementService clubMemberManagementService;

    @Test
    @DisplayName("changeMemberPosition은 자기 자신의 직책 변경을 거부한다")
    void changeMemberPositionRejectsSelfTarget() {
        // given
        Integer clubId = 1;
        when(clubRepository.getById(clubId)).thenReturn(createClub());

        // when & then
        assertErrorCode(
            () -> clubMemberManagementService.changeMemberPosition(
                clubId,
                10,
                10,
                new MemberPositionChangeRequest(ClubPosition.MANAGER)
            ),
            CANNOT_CHANGE_OWN_POSITION
        );
    }

    @Test
    @DisplayName("changeMemberPosition은 운영진 정원이 가득 차면 승격을 막는다")
    void changeMemberPositionRejectsManagerPromotionWhenLimitExceeded() {
        // given
        Integer clubId = 1;
        Integer requesterId = 100;
        Integer targetUserId = 200;
        Club club = createClub();
        User admin = createUser(requesterId, "관리자", UserRole.ADMIN);
        ClubMember targetMember = ClubMemberFixture.createMember(club, createUser(targetUserId, "대상", UserRole.USER));

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(userRepository.getById(requesterId)).thenReturn(admin);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, targetUserId)).thenReturn(targetMember);
        when(clubMemberRepository.countByClubIdAndPosition(clubId, ClubPosition.MANAGER))
            .thenReturn((long)ClubMemberManagementService.MAX_MANAGER_COUNT);

        // when & then
        assertErrorCode(
            () -> clubMemberManagementService.changeMemberPosition(
                clubId,
                targetUserId,
                requesterId,
                new MemberPositionChangeRequest(ClubPosition.MANAGER)
            ),
            MANAGER_LIMIT_EXCEEDED
        );
    }

    @Test
    @DisplayName("addPreMember는 동일 학번/이름으로 일치하는 유저가 여러 명이면 모호성 오류를 던진다")
    void addPreMemberRejectsAmbiguousUserMatches() {
        // given
        Integer clubId = 1;
        Integer requesterId = 10;
        Club club = createClub();
        ClubPreMemberAddRequest request = new ClubPreMemberAddRequest("20240001", "홍길동", ClubPosition.MEMBER);

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(
            userRepository.findAllByUniversityIdAndStudentNumber(
                club.getUniversity().getId(),
                request.studentNumber()
            )
        )
            .thenReturn(List.of(
                createUser(1, request.name(), UserRole.USER),
                createUser(2, request.name(), UserRole.USER)
            ));

        // when & then
        assertErrorCode(
            () -> clubMemberManagementService.addPreMember(clubId, requesterId, request),
            AMBIGUOUS_USER_MATCH
        );
    }

    @Test
    @DisplayName("addPreMember는 정확히 일치하는 유저가 한 명이면 즉시 회원으로 추가한다")
    void addPreMemberAddsDirectMemberWhenExactlyOneUserMatches() {
        // given
        Integer clubId = 1;
        Integer requesterId = 10;
        Club club = createClub();
        ClubPreMemberAddRequest request = new ClubPreMemberAddRequest("20240001", "홍길동", ClubPosition.MANAGER);
        User matchedUser = createUser(1, request.name(), UserRole.USER);

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(
            userRepository.findAllByUniversityIdAndStudentNumber(
                club.getUniversity().getId(),
                request.studentNumber()
            )
        )
            .thenReturn(List.of(matchedUser));
        when(clubMemberRepository.existsByClubIdAndUserId(clubId, matchedUser.getId())).thenReturn(false);
        when(clubMemberRepository.save(org.mockito.ArgumentMatchers.any(ClubMember.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ClubPreMemberAddResponse response = clubMemberManagementService.addPreMember(clubId, requesterId, request);

        // then
        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.studentNumber()).isEqualTo(request.studentNumber());
        assertThat(response.clubPosition()).isEqualTo(ClubPosition.MANAGER);
        verify(clubPreMemberRepository).deleteByClubIdAndStudentNumber(clubId, request.studentNumber());
        verify(chatRoomMembershipService).addClubMember(org.mockito.ArgumentMatchers.any(ClubMember.class));
    }

    private Club createClub() {
        var university = UniversityFixture.create();
        ReflectionTestUtils.setField(university, "id", 1);

        Club club = ClubFixture.create(university);
        ReflectionTestUtils.setField(club, "id", 1);

        return club;
    }

    private User createUser(Integer id, String name, UserRole role) {
        return User.builder()
            .id(id)
            .university(UniversityFixture.create())
            .email("user" + id + "@koreatech.ac.kr")
            .name(name)
            .studentNumber("2024" + String.format("%04d", id))
            .role(role)
            .isMarketingAgreement(true)
            .imageUrl("https://example.com/profile.png")
            .build();
    }

    private void assertErrorCode(ThrowingCallable callable, ApiResponseCode errorCode) {
        assertThatThrownBy(callable::call)
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode()).isEqualTo(errorCode));
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }
}
