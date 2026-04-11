package gg.agit.konect.unit.domain.club.service;

import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CLUB_MEMBER_ACCESS;
import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_ROLE_ACCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.chat.service.ChatRoomMembershipService;
import gg.agit.konect.domain.club.dto.ClubCondition;
import gg.agit.konect.domain.club.dto.ClubCreateRequest;
import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubMemberCondition;
import gg.agit.konect.domain.club.dto.ClubMembersResponse;
import gg.agit.konect.domain.club.dto.ClubMembershipsResponse;
import gg.agit.konect.domain.club.dto.ClubsResponse;
import gg.agit.konect.domain.club.dto.MyManagedClubResponse;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.enums.RecruitmentStatus;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubApplyQuestion;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubSummaryInfo;
import gg.agit.konect.domain.club.repository.ClubApplyQuestionRepository;
import gg.agit.konect.domain.club.repository.ClubApplyRepository;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubQueryRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.club.service.ClubPermissionValidator;
import gg.agit.konect.domain.club.service.ClubService;
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

class ClubServiceTest extends ServiceTestSupport {

    @Mock
    private ClubQueryRepository clubQueryRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private ClubApplyRepository clubApplyRepository;

    @Mock
    private ClubApplyQuestionRepository clubApplyQuestionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClubPermissionValidator clubPermissionValidator;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMembershipService chatRoomMembershipService;

    @InjectMocks
    private ClubService clubService;

    @Test
    @DisplayName("createClub은 관리자가 동아리를 생성하면 기본 질문과 회장 멤버십까지 함께 만든다")
    void createClubCreatesClubPresidentAndDefaultQuestions() {
        // given
        Integer adminUserId = 1;
        Integer presidentUserId = 2;
        User admin = UserFixture.createUserWithId(adminUserId, "관리자", UserRole.ADMIN);
        User presidentUser = UserFixture.createUserWithId(presidentUserId, "회장", UserRole.USER);
        ClubCreateRequest request = new ClubCreateRequest(
            presidentUserId,
            "KONECT",
            "테스트 동아리",
            "상세 소개",
            "https://example.com/club.png",
            "학생회관 101호",
            ClubCategory.ACADEMIC
        );
        Club savedClub = request.toEntity(presidentUser.getUniversity());
        ReflectionTestUtils.setField(savedClub, "id", 100);
        ClubMember savedPresident = ClubMemberFixture.createPresident(savedClub, presidentUser);

        given(userRepository.getById(adminUserId)).willReturn(admin);
        given(userRepository.getById(presidentUserId)).willReturn(presidentUser);
        given(clubRepository.save(any(Club.class))).willReturn(savedClub);
        given(clubMemberRepository.save(any(ClubMember.class))).willReturn(savedPresident);
        given(clubRepository.getById(savedClub.getId())).willReturn(savedClub);
        given(clubMemberRepository.findAllByClubId(savedClub.getId())).willReturn(List.of(savedPresident));
        given(clubApplyRepository.existsPendingByClubIdAndUserId(savedClub.getId(), adminUserId)).willReturn(false);

        // when
        ClubDetailResponse response = clubService.createClub(adminUserId, request);

        // then
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(clubMemberRepository).save(argThat(clubMember ->
            clubMember.getClub().equals(savedClub)
                && clubMember.getUser().equals(presidentUser)
                && clubMember.getClubPosition() == ClubPosition.PRESIDENT
        ));
        verify(chatRoomMembershipService).addClubMember(savedPresident);

        ArgumentCaptor<List<ClubApplyQuestion>> questionCaptor = ArgumentCaptor.forClass(List.class);
        verify(clubApplyQuestionRepository).saveAll(questionCaptor.capture());
        assertThat(questionCaptor.getValue())
            .extracting(ClubApplyQuestion::getQuestion, ClubApplyQuestion::getIsRequired, ClubApplyQuestion::getDisplayOrder)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("본인의 전화번호를 입력해주세요.", true, 1),
                org.assertj.core.groups.Tuple.tuple("지원 동기", false, 2)
            );

        assertThat(response.id()).isEqualTo(savedClub.getId());
        assertThat(response.presidentUserId()).isEqualTo(presidentUserId);
        assertThat(response.isMember()).isFalse();
        assertThat(response.isApplied()).isFalse();
        assertThat(response.recruitment().status()).isEqualTo(RecruitmentStatus.CLOSED);
    }

    @Test
    @DisplayName("createClub은 관리자가 아니면 접근을 거부한다")
    void createClubRejectsNonAdminRequester() {
        // given
        Integer userId = 1;
        User user = UserFixture.createUserWithId(userId, "일반 사용자", UserRole.USER);
        ClubCreateRequest request = new ClubCreateRequest(
            2,
            "KONECT",
            "테스트 동아리",
            "상세 소개",
            "https://example.com/club.png",
            "학생회관 101호",
            ClubCategory.ACADEMIC
        );
        given(userRepository.getById(userId)).willReturn(user);

        // when & then
        assertErrorCode(() -> clubService.createClub(userId, request), FORBIDDEN_ROLE_ACCESS);
        verify(clubRepository, never()).save(any(Club.class));
    }

    @Test
    @DisplayName("getClubs는 지원 중이지만 아직 회원이 아닌 동아리만 pending 으로 표시한다")
    void getClubsReturnsOnlyPendingAppliedNonMemberClubs() {
        // given
        Integer userId = 10;
        User user = UserFixture.createUserWithId(UniversityFixture.createWithId(1), userId, "사용자", "20240010", UserRole.USER);
        ClubCondition condition = new ClubCondition(1, 10, "", false);
        ClubSummaryInfo appliedOnlyClub = new ClubSummaryInfo(
            101,
            "대기 동아리",
            "https://example.com/club-1.png",
            "학술",
            "설명",
            RecruitmentStatus.ONGOING,
            false,
            null
        );
        ClubSummaryInfo joinedClub = new ClubSummaryInfo(
            102,
            "가입 동아리",
            "https://example.com/club-2.png",
            "학술",
            "설명",
            RecruitmentStatus.ONGOING,
            false,
            null
        );
        Page<ClubSummaryInfo> page = new PageImpl<>(
            List.of(appliedOnlyClub, joinedClub),
            PageRequest.of(0, 10),
            2
        );

        given(userRepository.getById(userId)).willReturn(user);
        given(clubQueryRepository.findAllByFilter(PageRequest.of(0, 10), "", false, user.getUniversity().getId()))
            .willReturn(page);
        given(clubApplyRepository.findClubIdsByUserIdAndClubIdIn(userId, List.of(101, 102)))
            .willReturn(List.of(101, 102));
        given(clubMemberRepository.findClubIdsByUserIdAndClubIdIn(userId, List.of(101, 102)))
            .willReturn(List.of(102));

        // when
        ClubsResponse response = clubService.getClubs(condition, userId);

        // then
        assertThat(response.currentCount()).isEqualTo(2);
        assertThat(response.clubs())
            .extracting(ClubsResponse.InnerClubResponse::id, ClubsResponse.InnerClubResponse::isPendingApproval)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(101, true),
                org.assertj.core.groups.Tuple.tuple(102, false)
            );
    }

    @Test
    @DisplayName("getClubs는 조회 결과가 비어 있으면 pending 집합도 비운다")
    void getClubsReturnsEmptyPendingWhenNoClubExists() {
        // given
        Integer userId = 10;
        User user = UserFixture.createUserWithId(UniversityFixture.createWithId(1), userId, "사용자", "20240010", UserRole.USER);
        ClubCondition condition = new ClubCondition(1, 10, null, null);
        Page<ClubSummaryInfo> emptyPage = Page.empty(PageRequest.of(0, 10));

        given(userRepository.getById(userId)).willReturn(user);
        given(clubQueryRepository.findAllByFilter(PageRequest.of(0, 10), "", false, user.getUniversity().getId()))
            .willReturn(emptyPage);

        // when
        ClubsResponse response = clubService.getClubs(condition, userId);

        // then
        assertThat(response.clubs()).isEmpty();
        verify(clubApplyRepository, never()).findClubIdsByUserIdAndClubIdIn(any(), any());
        verify(clubMemberRepository, never()).findClubIdsByUserIdAndClubIdIn(any(), any());
    }

    @Test
    @DisplayName("getClubDetail은 회원 여부와 pending 지원 여부를 함께 계산한다")
    void getClubDetailCombinesMembershipAndPendingApplication() {
        // given
        Integer clubId = 1;
        Integer userId = 20;
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD");
        User presidentUser = UserFixture.createUserWithId(1, "회장", UserRole.USER);
        User memberUser = UserFixture.createUserWithId(2, "회원", UserRole.USER);
        ClubMember president = ClubMemberFixture.createPresident(club, presidentUser);
        ClubMember member = ClubMemberFixture.createMember(club, memberUser);

        given(clubRepository.getById(clubId)).willReturn(club);
        given(clubMemberRepository.findAllByClubId(clubId)).willReturn(List.of(president, member));
        given(clubApplyRepository.existsPendingByClubIdAndUserId(clubId, userId)).willReturn(true);

        // when
        ClubDetailResponse response = clubService.getClubDetail(clubId, userId);

        // then
        assertThat(response.memberCount()).isEqualTo(2);
        assertThat(response.presidentUserId()).isEqualTo(presidentUser.getId());
        assertThat(response.isMember()).isFalse();
        assertThat(response.isApplied()).isTrue();
    }

    @Test
    @DisplayName("getManagedClubs는 관리자는 전체 동아리를, 일반 사용자는 manager 이상 소속만 반환한다")
    void getManagedClubsReturnsAllForAdminAndManagerOnlyForUser() {
        // given
        Integer adminId = 1;
        Integer managerId = 2;
        User admin = UserFixture.createUserWithId(adminId, "관리자", UserRole.ADMIN);
        User manager = UserFixture.createUserWithId(managerId, "운영진", UserRole.USER);
        Club allClub = ClubFixture.createWithId(UniversityFixture.createWithId(1), 100, "전체 동아리");
        Club managedClub = ClubFixture.createWithId(UniversityFixture.createWithId(1), 200, "운영 동아리");
        ClubMember managerMember = ClubMemberFixture.createManager(managedClub, manager);

        given(userRepository.getById(adminId)).willReturn(admin);
        given(userRepository.getById(managerId)).willReturn(manager);
        given(clubRepository.findAll()).willReturn(List.of(allClub));
        given(clubMemberRepository.findAllByUserIdAndClubPositions(managerId, ClubPosition.MANAGERS))
            .willReturn(List.of(managerMember));

        // when
        ClubMembershipsResponse adminResponse = clubService.getManagedClubs(adminId);
        ClubMembershipsResponse managerResponse = clubService.getManagedClubs(managerId);

        // then
        assertThat(adminResponse.joinedClubs())
            .extracting(ClubMembershipsResponse.InnerJoinedClubResponse::id)
            .containsExactly(allClub.getId());
        assertThat(managerResponse.joinedClubs())
            .extracting(ClubMembershipsResponse.InnerJoinedClubResponse::id,
                ClubMembershipsResponse.InnerJoinedClubResponse::position)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(managedClub.getId(), ClubPosition.MANAGER.getDescription())
            );
    }

    @Test
    @DisplayName("getManagedClubDetail은 일반 사용자에게 manager 권한 검증 후 상세 정보를 반환한다")
    void getManagedClubDetailValidatesManagerAccessForUser() {
        // given
        Integer clubId = 1;
        Integer userId = 10;
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD");
        User user = UserFixture.createUserWithId(userId, "운영진", UserRole.USER);
        ClubMember clubMember = ClubMemberFixture.createManager(club, user);

        given(clubRepository.getById(clubId)).willReturn(club);
        given(userRepository.getById(userId)).willReturn(user);
        given(clubMemberRepository.getByClubIdAndUserId(clubId, userId)).willReturn(clubMember);

        // when
        MyManagedClubResponse response = clubService.getManagedClubDetail(clubId, userId);

        // then
        verify(clubPermissionValidator).validateManagerAccess(clubId, userId);
        assertThat(response.clubId()).isEqualTo(clubId);
        assertThat(response.name()).isEqualTo(user.getName());
        assertThat(response.position()).isEqualTo(ClubPosition.MANAGER.getDescription());
    }

    @Test
    @DisplayName("getClubMembers는 admin과 manager에게는 학번을 그대로 보여준다")
    void getClubMembersReturnsUnmaskedStudentNumbersForAdminAndManager() {
        // given
        Integer clubId = 1;
        User admin = UserFixture.createUserWithId(1, "관리자", UserRole.ADMIN);
        User managerUser = UserFixture.createUserWithId(2, "운영진", UserRole.USER);
        User memberUser = UserFixture.createUserWithId(3, "회원", UserRole.USER);
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD");
        ClubMember managerMember = ClubMemberFixture.createManager(club, managerUser);
        ClubMember member = ClubMemberFixture.createMember(club, memberUser);
        ClubMemberCondition condition = new ClubMemberCondition(ClubPosition.MEMBER);

        given(userRepository.getById(admin.getId())).willReturn(admin);
        given(userRepository.getById(managerUser.getId())).willReturn(managerUser);
        given(clubMemberRepository.findByClubIdAndUserId(clubId, managerUser.getId())).willReturn(java.util.Optional.of(managerMember));
        given(clubMemberRepository.findAllByClubIdAndPosition(clubId, ClubPosition.MEMBER)).willReturn(List.of(member));

        // when
        ClubMembersResponse adminResponse = clubService.getClubMembers(clubId, admin.getId(), condition);
        ClubMembersResponse managerResponse = clubService.getClubMembers(clubId, managerUser.getId(), condition);

        // then
        assertThat(adminResponse.clubMembers())
            .extracting(ClubMembersResponse.InnerClubMember::studentNumber)
            .containsExactly(memberUser.getStudentNumber());
        assertThat(managerResponse.clubMembers())
            .extracting(ClubMembersResponse.InnerClubMember::studentNumber)
            .containsExactly(memberUser.getStudentNumber());
    }

    @Test
    @DisplayName("getClubMembers는 일반 회원에게는 마스킹된 학번을 반환하고 비회원은 거부한다")
    void getClubMembersMasksStudentNumberForMemberAndRejectsNonMember() {
        // given
        Integer clubId = 1;
        Integer memberUserId = 10;
        Integer outsiderUserId = 20;
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD");
        User memberUser = UserFixture.createUserWithId(UniversityFixture.createWithId(1), memberUserId, "일반 회원", "20241234", UserRole.USER);
        User targetUser = UserFixture.createUserWithId(UniversityFixture.createWithId(1), 30, "조회 대상", "20249876", UserRole.USER);
        User outsider = UserFixture.createUserWithId(outsiderUserId, "외부인", UserRole.USER);
        ClubMember requesterMember = ClubMemberFixture.createMember(club, memberUser);
        ClubMember targetMember = ClubMemberFixture.createMember(club, targetUser);

        given(userRepository.getById(memberUserId)).willReturn(memberUser);
        given(userRepository.getById(outsiderUserId)).willReturn(outsider);
        given(clubMemberRepository.findByClubIdAndUserId(clubId, memberUserId)).willReturn(java.util.Optional.of(requesterMember));
        given(clubMemberRepository.findByClubIdAndUserId(clubId, outsiderUserId)).willReturn(java.util.Optional.empty());
        given(clubMemberRepository.findAllByClubId(clubId)).willReturn(List.of(targetMember));

        // when
        ClubMembersResponse response = clubService.getClubMembers(clubId, memberUserId, null);

        // then
        assertThat(response.clubMembers())
            .extracting(ClubMembersResponse.InnerClubMember::studentNumber)
            .containsExactly("*****876");

        assertErrorCode(
            () -> clubService.getClubMembers(clubId, outsiderUserId, null),
            FORBIDDEN_CLUB_MEMBER_ACCESS
        );
    }

    private void assertErrorCode(ThrowingCallable callable, ApiResponseCode errorCode) {
        assertThatThrownBy(callable)
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode()).isEqualTo(errorCode));
    }
}
