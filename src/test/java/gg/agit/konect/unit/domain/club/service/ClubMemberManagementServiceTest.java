package gg.agit.konect.unit.domain.club.service;

import static gg.agit.konect.global.code.ApiResponseCode.AMBIGUOUS_USER_MATCH;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_CHANGE_OWN_POSITION;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_DELETE_CLUB_PRESIDENT;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_MANAGE_HIGHER_POSITION;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_REMOVE_NON_MEMBER;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_REMOVE_SELF;
import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_MEMBER_POSITION_CHANGE;
import static gg.agit.konect.global.code.ApiResponseCode.ILLEGAL_ARGUMENT;
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
import org.springframework.transaction.PlatformTransactionManager;

import gg.agit.konect.domain.chat.service.ChatRoomMembershipService;
import gg.agit.konect.domain.club.dto.ClubPreMemberAddRequest;
import gg.agit.konect.domain.club.dto.ClubPreMemberAddResponse;
import gg.agit.konect.domain.club.dto.ClubPreMemberBatchAddRequest;
import gg.agit.konect.domain.club.dto.MemberPositionChangeRequest;
import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.club.service.ClubMemberManagementService;
import gg.agit.konect.domain.club.service.ClubPermissionValidator;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

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
        User admin = UserFixture.createUserWithId(requesterId, "관리자", UserRole.ADMIN);
        ClubMember targetMember = ClubMemberFixture.createMember(club,
            UserFixture.createUserWithId(targetUserId, "대상", UserRole.USER));

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
                UserFixture.createUserWithId(1, request.name(), UserRole.USER),
                UserFixture.createUserWithId(2, request.name(), UserRole.USER)
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
        User matchedUser = UserFixture.createUserWithId(1, request.name(), UserRole.USER);

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
        assertThat(response.studentNumber()).isEqualTo(matchedUser.getStudentNumber());
        assertThat(response.clubPosition()).isEqualTo(ClubPosition.MANAGER);
        verify(clubPreMemberRepository).deleteByClubIdAndStudentNumber(clubId, request.studentNumber());
        verify(chatRoomMembershipService).addClubMember(org.mockito.ArgumentMatchers.any(ClubMember.class));
    }

    @Test
    @DisplayName("changeMemberPosition은 관리자가 아닌 사용자의 canManage 검증에 실패하면 예외를 던진다")
    void changeMemberPositionValidatesRequesterCanManageTarget() {
        // given
        Integer clubId = 1;
        Integer requesterId = 100;
        Integer targetUserId = 200;
        Club club = createClub();
        User requesterUser = UserFixture.createUserWithId(requesterId, "요청자", UserRole.USER);
        ClubMember requesterMember = ClubMemberFixture.createMember(club, requesterUser);
        User targetUser = UserFixture.createUserWithId(targetUserId, "대상", UserRole.USER);
        ClubMember targetMember = ClubMemberFixture.createManager(club, targetUser);

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(userRepository.getById(requesterId)).thenReturn(requesterUser);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, requesterId)).thenReturn(requesterMember);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, targetUserId)).thenReturn(targetMember);

        // when & then
        assertErrorCode(
            () -> clubMemberManagementService.changeMemberPosition(
                clubId,
                targetUserId,
                requesterId,
                new MemberPositionChangeRequest(ClubPosition.MEMBER)
            ),
            CANNOT_MANAGE_HIGHER_POSITION
        );
    }

    @Test
    @DisplayName("changeMemberPosition은 관리자의 canManage(newPosition) 검증에 실패하면 예외를 던진다")
    void changeMemberPositionValidatesRequesterCanManageNewPosition() {
        // given
        Integer clubId = 1;
        Integer requesterId = 100;
        Integer targetUserId = 200;
        Club club = createClub();
        User requesterUser = UserFixture.createUserWithId(requesterId, "요청자", UserRole.USER);
        ClubMember requesterMember = ClubMemberFixture.createVicePresident(club, requesterUser); // 부회장이 요청
        User targetUser = UserFixture.createUserWithId(targetUserId, "대상", UserRole.USER);
        ClubMember targetMember = ClubMemberFixture.createMember(club, targetUser); // 일반 회원을 부회장으로 승격 시도

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(userRepository.getById(requesterId)).thenReturn(requesterUser);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, requesterId)).thenReturn(requesterMember);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, targetUserId)).thenReturn(targetMember);

        // when & then
        assertErrorCode(
            () -> clubMemberManagementService.changeMemberPosition(
                clubId,
                targetUserId,
                requesterId,
                new MemberPositionChangeRequest(ClubPosition.PRESIDENT) // 회장으로 승격 시도 (부회장 권한 밖)
            ),
            FORBIDDEN_MEMBER_POSITION_CHANGE
        );
    }

    @Test
    @DisplayName("getPreMembers는 정상 동작한다")
    void getPreMembersWorksNormally() {
        // given
        Integer clubId = 1;
        Integer requesterId = 10;
        Club club = createClub();
        gg.agit.konect.domain.club.model.ClubPreMember preMember1 = gg.agit.konect.domain.club.model.ClubPreMember.builder()
            .id(1)
            .club(club)
            .studentNumber("20240001")
            .name("홍길동")
            .clubPosition(ClubPosition.MEMBER)
            .build();
        gg.agit.konect.domain.club.model.ClubPreMember preMember2 = gg.agit.konect.domain.club.model.ClubPreMember.builder()
            .id(2)
            .club(club)
            .studentNumber("20240002")
            .name("김철수")
            .clubPosition(ClubPosition.MANAGER)
            .build();

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(clubPreMemberRepository.findAllByClubId(clubId)).thenReturn(List.of(preMember1, preMember2));

        // when
        gg.agit.konect.domain.club.dto.ClubPreMembersResponse response = clubMemberManagementService.getPreMembers(
            clubId, requesterId);

        // then
        assertThat(response.preMembers()).hasSize(2);
    }

    @Test
    @DisplayName("removePreMember는 정상 동작한다")
    void removePreMemberWorksNormally() {
        // given
        Integer clubId = 1;
        Integer preMemberId = 5;
        Integer requesterId = 10;
        Club club = createClub();
        gg.agit.konect.domain.club.model.ClubPreMember preMember = gg.agit.konect.domain.club.model.ClubPreMember.builder()
            .id(preMemberId)
            .club(club)
            .studentNumber("20240001")
            .name("홍길동")
            .clubPosition(ClubPosition.MEMBER)
            .build();

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(clubPreMemberRepository.getByIdAndClubId(preMemberId, clubId)).thenReturn(preMember);

        // when
        clubMemberManagementService.removePreMember(clubId, preMemberId, requesterId);

        // then
        verify(clubPreMemberRepository).delete(preMember);
    }

    @Test
    @DisplayName("transferPresident는 자기 자신에게 이전 시도하면 예외를 던진다")
    void transferPresidentValidatesNotSelf() {
        // given
        Integer clubId = 1;
        Integer currentPresidentId = 100;
        Club club = createClub();
        User presidentUser = UserFixture.createUserWithId(currentPresidentId, "회장", UserRole.USER);
        ClubMember president = ClubMemberFixture.createPresident(club, presidentUser);
        gg.agit.konect.domain.club.dto.PresidentTransferRequest request = new gg.agit.konect.domain.club.dto.PresidentTransferRequest(
            currentPresidentId);

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, currentPresidentId)).thenReturn(president);

        // when & then
        assertErrorCode(
            () -> clubMemberManagementService.transferPresident(clubId, currentPresidentId, request),
            ILLEGAL_ARGUMENT
        );
    }

    @Test
    @DisplayName("transferPresident는 정상 이전 동작을 수행한다")
    void transferPresidentWorksNormally() {
        // given
        Integer clubId = 1;
        Integer currentPresidentId = 100;
        Integer newPresidentId = 200;
        Club club = createClub();
        User currentPresidentUser = UserFixture.createUserWithId(currentPresidentId, "현회장", UserRole.USER);
        User newPresidentUser = UserFixture.createUserWithId(newPresidentId, "신회장", UserRole.USER);
        ClubMember currentPresident = ClubMemberFixture.createPresident(club, currentPresidentUser);
        ClubMember newPresident = ClubMemberFixture.createMember(club, newPresidentUser);
        gg.agit.konect.domain.club.dto.PresidentTransferRequest request = new gg.agit.konect.domain.club.dto.PresidentTransferRequest(
            newPresidentId);

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, currentPresidentId)).thenReturn(currentPresident);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, newPresidentId)).thenReturn(newPresident);

        // when
        List<ClubMember> result = clubMemberManagementService.transferPresident(clubId, currentPresidentId, request);

        // then
        assertThat(result).hasSize(2);
        assertThat(currentPresident.getClubPosition()).isEqualTo(ClubPosition.MEMBER);
        assertThat(newPresident.getClubPosition()).isEqualTo(ClubPosition.PRESIDENT);
    }

    @Test
    @DisplayName("changeVicePresident는 기존 부회장이 없는 경우 null VP 요청을 처리한다")
    void changeVicePresidentHandlesNullVpWhenNoExistingVp() {
        // given
        Integer clubId = 1;
        Integer requesterId = 100;
        Club club = createClub();
        User presidentUser = UserFixture.createUserWithId(requesterId, "회장", UserRole.USER);
        ClubMemberFixture.createPresident(club, presidentUser);
        gg.agit.konect.domain.club.dto.VicePresidentChangeRequest request = new gg.agit.konect.domain.club.dto.VicePresidentChangeRequest(
            null);

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(clubMemberRepository.findAllByClubIdAndPosition(clubId, ClubPosition.VICE_PRESIDENT)).thenReturn(
            List.of());

        // when
        List<ClubMember> result = clubMemberManagementService.changeVicePresident(clubId, requesterId, request);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("changeVicePresident는 기존 부회장을 다른 사용자로 교체한다")
    void changeVicePresidentReplacesExistingVp() {
        // given
        Integer clubId = 1;
        Integer requesterId = 100;
        Integer currentVpId = 200;
        Integer newVpId = 300;
        Club club = createClub();
        User presidentUser = UserFixture.createUserWithId(requesterId, "회장", UserRole.USER);
        User currentVpUser = UserFixture.createUserWithId(currentVpId, "현부회장", UserRole.USER);
        User newVpUser = UserFixture.createUserWithId(newVpId, "신부회장", UserRole.USER);
        ClubMember president = ClubMemberFixture.createPresident(club, presidentUser);
        ClubMember currentVp = ClubMemberFixture.createVicePresident(club, currentVpUser);
        ClubMember newVp = ClubMemberFixture.createMember(club, newVpUser);
        gg.agit.konect.domain.club.dto.VicePresidentChangeRequest request = new gg.agit.konect.domain.club.dto.VicePresidentChangeRequest(
            newVpId);

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(clubMemberRepository.findAllByClubIdAndPosition(clubId, ClubPosition.VICE_PRESIDENT)).thenReturn(
            List.of(currentVp));
        when(clubMemberRepository.getByClubIdAndUserId(clubId, newVpId)).thenReturn(newVp);

        // when
        List<ClubMember> result = clubMemberManagementService.changeVicePresident(clubId, requesterId, request);

        // then
        assertThat(result).hasSize(2);
        assertThat(currentVp.getClubPosition()).isEqualTo(ClubPosition.MEMBER);
        assertThat(newVp.getClubPosition()).isEqualTo(ClubPosition.VICE_PRESIDENT);
    }

    @Test
    @DisplayName("removeMember는 회장 제거 시도를 거부한다")
    void removeMemberRejectsPresidentRemoval() {
        // given
        Integer clubId = 1;
        Integer requesterId = 100;
        Integer targetUserId = 200;
        Club club = createClub();
        User requesterUser = UserFixture.createUserWithId(requesterId, "회장", UserRole.USER);
        User targetUser = UserFixture.createUserWithId(targetUserId, "대상", UserRole.USER);
        ClubMember target = ClubMemberFixture.createPresident(club, targetUser);

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(userRepository.getById(requesterId)).thenReturn(requesterUser);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, targetUserId)).thenReturn(target);

        // when & then
        assertErrorCode(
            () -> clubMemberManagementService.removeMember(clubId, targetUserId, requesterId),
            CANNOT_DELETE_CLUB_PRESIDENT
        );
    }

    @Test
    @DisplayName("removeMember는 비회원 제거 시도를 거부한다")
    void removeMemberRejectsNonMemberRemoval() {
        // given
        Integer clubId = 1;
        Integer requesterId = 100;
        Integer targetUserId = 200;
        Club club = createClub();
        User requesterUser = UserFixture.createUserWithId(requesterId, "회장", UserRole.USER);
        User targetUser = UserFixture.createUserWithId(targetUserId, "대상", UserRole.USER);
        ClubMember requester = ClubMemberFixture.createPresident(club, requesterUser);
        ClubMember target = ClubMemberFixture.createVicePresident(club, targetUser);

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(userRepository.getById(requesterId)).thenReturn(requesterUser);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, requesterId)).thenReturn(requester);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, targetUserId)).thenReturn(target);

        // when & then
        assertErrorCode(
            () -> clubMemberManagementService.removeMember(clubId, targetUserId, requesterId),
            CANNOT_REMOVE_NON_MEMBER
        );
    }

    @Test
    @DisplayName("removeMember는 정상 제거 동작을 수행한다")
    void removeMemberWorksNormally() {
        // given
        Integer clubId = 1;
        Integer requesterId = 100;
        Integer targetUserId = 200;
        Club club = createClub();
        User requesterUser = UserFixture.createUserWithId(requesterId, "회장", UserRole.USER);
        User targetUser = UserFixture.createUserWithId(targetUserId, "대상", UserRole.USER);
        ClubMember requester = ClubMemberFixture.createPresident(club, requesterUser);
        ClubMember target = ClubMemberFixture.createMember(club, targetUser);

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(userRepository.getById(requesterId)).thenReturn(requesterUser);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, requesterId)).thenReturn(requester);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, targetUserId)).thenReturn(target);

        // when
        clubMemberManagementService.removeMember(clubId, targetUserId, requesterId);

        // then
        verify(clubMemberRepository).delete(target);
        verify(chatRoomMembershipService).removeClubMember(clubId, targetUserId);
    }

    @Test
    @DisplayName("changeVicePresident는 같은 부회장 재지정 시 아무 변화 없음")
    void changeVicePresidentHandlesSameVicePresident() {
        // given
        Integer clubId = 1;
        Integer requesterId = 100;
        Integer currentVpId = 200;
        Club club = createClub();
        User presidentUser = UserFixture.createUserWithId(requesterId, "회장", UserRole.USER);
        User currentVpUser = UserFixture.createUserWithId(currentVpId, "부회장", UserRole.USER);
        ClubMemberFixture.createPresident(club, presidentUser);
        ClubMember currentVp = ClubMemberFixture.createVicePresident(club, currentVpUser);
        gg.agit.konect.domain.club.dto.VicePresidentChangeRequest request = new gg.agit.konect.domain.club.dto.VicePresidentChangeRequest(
            currentVpId);

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(clubMemberRepository.findAllByClubIdAndPosition(clubId, ClubPosition.VICE_PRESIDENT)).thenReturn(
            List.of(currentVp));
        when(clubMemberRepository.getByClubIdAndUserId(clubId, currentVpId)).thenReturn(currentVp);

        // when
        List<ClubMember> result = clubMemberManagementService.changeVicePresident(clubId, requesterId, request);

        // then
        assertThat(result).hasSize(1);
        assertThat(currentVp.getClubPosition()).isEqualTo(ClubPosition.VICE_PRESIDENT);
    }

    @Test
    @DisplayName("changeVicePresident는 기존 VP 강등 후 새 VP 지정")
    void changeVicePresidentDemotesOldVpAndAppointsNewVp() {
        // given
        Integer clubId = 1;
        Integer requesterId = 100;
        Integer currentVpId = 200;
        Integer newVpId = 300;
        Club club = createClub();
        User presidentUser = UserFixture.createUserWithId(requesterId, "회장", UserRole.USER);
        User currentVpUser = UserFixture.createUserWithId(currentVpId, "현부회장", UserRole.USER);
        User newVpUser = UserFixture.createUserWithId(newVpId, "신부회장", UserRole.USER);
        ClubMember president = ClubMemberFixture.createPresident(club, presidentUser);
        ClubMember currentVp = ClubMemberFixture.createVicePresident(club, currentVpUser);
        ClubMember newVp = ClubMemberFixture.createMember(club, newVpUser);
        gg.agit.konect.domain.club.dto.VicePresidentChangeRequest request = new gg.agit.konect.domain.club.dto.VicePresidentChangeRequest(
            newVpId);

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(clubMemberRepository.findAllByClubIdAndPosition(clubId, ClubPosition.VICE_PRESIDENT)).thenReturn(
            List.of(currentVp));
        when(clubMemberRepository.getByClubIdAndUserId(clubId, newVpId)).thenReturn(newVp);

        // when
        List<ClubMember> result = clubMemberManagementService.changeVicePresident(clubId, requesterId, request);

        // then
        assertThat(result).hasSize(2);
        assertThat(currentVp.getClubPosition()).isEqualTo(ClubPosition.MEMBER);
        assertThat(newVp.getClubPosition()).isEqualTo(ClubPosition.VICE_PRESIDENT);
    }

    @Test
    @DisplayName("transferPresident는 이전 후 기존 회장이 MEMBER로 변경됨")
    void transferPresidentChangesOldPresidentToMember() {
        // given
        Integer clubId = 1;
        Integer currentPresidentId = 100;
        Integer newPresidentId = 200;
        Club club = createClub();
        User currentPresidentUser = UserFixture.createUserWithId(currentPresidentId, "현회장", UserRole.USER);
        User newPresidentUser = UserFixture.createUserWithId(newPresidentId, "신회장", UserRole.USER);
        ClubMember currentPresident = ClubMemberFixture.createPresident(club, currentPresidentUser);
        ClubMember newPresident = ClubMemberFixture.createManager(club, newPresidentUser);
        gg.agit.konect.domain.club.dto.PresidentTransferRequest request = new gg.agit.konect.domain.club.dto.PresidentTransferRequest(
            newPresidentId);

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, currentPresidentId)).thenReturn(currentPresident);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, newPresidentId)).thenReturn(newPresident);

        // when
        List<ClubMember> result = clubMemberManagementService.transferPresident(clubId, currentPresidentId, request);

        // then
        assertThat(result).hasSize(2);
        assertThat(currentPresident.getClubPosition()).isEqualTo(ClubPosition.MEMBER);
        assertThat(newPresident.getClubPosition()).isEqualTo(ClubPosition.PRESIDENT);
    }

    @Test
    @DisplayName("removeMember는 자기 자신 제거 시도를 거부한다")
    void removeMemberRejectsSelfRemoval() {
        // given
        Integer clubId = 1;
        Integer requesterId = 100;
        Club club = createClub();

        when(clubRepository.getById(clubId)).thenReturn(club);

        // when & then
        assertErrorCode(
            () -> clubMemberManagementService.removeMember(clubId, requesterId, requesterId),
            CANNOT_REMOVE_SELF
        );
    }

    @Test
    @DisplayName("changeMemberPosition은 VP에서 VP로 변경 시도 시 validatePositionLimit 통과")
    void changeMemberPositionAllowsVpToVpChange() {
        // given
        Integer clubId = 1;
        Integer requesterId = 100;
        Integer targetUserId = 200;
        Club club = createClub();
        User admin = UserFixture.createUserWithId(requesterId, "관리자", UserRole.ADMIN);
        ClubMember targetMember = ClubMemberFixture.createVicePresident(club,
            UserFixture.createUserWithId(targetUserId, "대상", UserRole.USER));

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(userRepository.getById(requesterId)).thenReturn(admin);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, targetUserId)).thenReturn(targetMember);

        // when
        ClubMember result = clubMemberManagementService.changeMemberPosition(
            clubId,
            targetUserId,
            requesterId,
            new MemberPositionChangeRequest(ClubPosition.VICE_PRESIDENT)
        );

        // then
        assertThat(result.getClubPosition()).isEqualTo(ClubPosition.VICE_PRESIDENT);
    }

    @Test
    @DisplayName("addPreMembersBatch는 기존 멤버가 포함된 경우 스킽한다")
    void addPreMembersBatchSkipsExistingMembers() {
        // given
        Integer clubId = 1;
        Integer requesterId = 10;
        Club club = createClub();
        ClubPreMemberBatchAddRequest request = new ClubPreMemberBatchAddRequest(List.of(
            new ClubPreMemberAddRequest("20240001", "홍길동", ClubPosition.MEMBER),
            new ClubPreMemberAddRequest("20240002", "김철수", ClubPosition.MANAGER)
        ));
        User existingUser = UserFixture.createUserWithId(UniversityFixture.create(), 1, "홍길동", "20240001",
            UserRole.USER);

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(userRepository.findAllByUniversityIdAndStudentNumber(club.getUniversity().getId(), "20240001"))
            .thenReturn(List.of(existingUser));
        when(clubMemberRepository.existsByClubIdAndUserId(clubId, existingUser.getId())).thenReturn(true);
        when(userRepository.findAllByUniversityIdAndStudentNumber(club.getUniversity().getId(), "20240002"))
            .thenReturn(List.of());

        // when
        gg.agit.konect.domain.club.dto.ClubPreMemberBatchAddResponse response =
            clubMemberManagementService.addPreMembersBatch(clubId, requesterId, request);

        // then
        assertThat(response.results()).hasSize(2);
        assertThat(response.results().get(0).success()).isFalse();
        assertThat(response.results().get(0).errorCode()).isEqualTo("ALREADY_CLUB_MEMBER");
    }

    @Test
    @DisplayName("removeMember는 관리자가 관리자 제거 시도를 거부한다")
    void removeMemberRejectsManagerRemovingManager() {
        // given
        Integer clubId = 1;
        Integer requesterId = 100;
        Integer targetUserId = 200;
        Club club = createClub();
        User requesterUser = UserFixture.createUserWithId(requesterId, "요청자", UserRole.USER);
        User targetUser = UserFixture.createUserWithId(targetUserId, "대상", UserRole.USER);
        ClubMember requester = ClubMemberFixture.createManager(club, requesterUser);
        ClubMember target = ClubMemberFixture.createManager(club, targetUser);

        when(clubRepository.getById(clubId)).thenReturn(club);
        when(userRepository.getById(requesterId)).thenReturn(requesterUser);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, requesterId)).thenReturn(requester);
        when(clubMemberRepository.getByClubIdAndUserId(clubId, targetUserId)).thenReturn(target);

        // when & then
        assertErrorCode(
            () -> clubMemberManagementService.removeMember(clubId, targetUserId, requesterId),
            CANNOT_MANAGE_HIGHER_POSITION
        );
    }

    private Club createClub() {
        return ClubFixture.createWithId(UniversityFixture.createWithId(1), 1);
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
