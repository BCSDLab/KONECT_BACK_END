package gg.agit.konect.unit.domain.club.service;

import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CLUB_MEMBER_ACCESS;
import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_ROLE_ACCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;
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
import gg.agit.konect.domain.club.dto.ClubBasicInfoUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubCondition;
import gg.agit.konect.domain.club.dto.ClubCreateRequest;
import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubMemberCondition;
import gg.agit.konect.domain.club.dto.ClubMembersResponse;
import gg.agit.konect.domain.club.dto.ClubMembershipsResponse;
import gg.agit.konect.domain.club.dto.ClubUpdateRequest;
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
            .extracting(ClubApplyQuestion::getQuestion, ClubApplyQuestion::getIsRequired,
                ClubApplyQuestion::getDisplayOrder)
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
        User user = UserFixture.createUserWithId(UniversityFixture.createWithId(1), userId, "사용자", "20240010",
            UserRole.USER);
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
        User user = UserFixture.createUserWithId(UniversityFixture.createWithId(1), userId, "사용자", "20240010",
            UserRole.USER);
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
        verify(clubPermissionValidator).validateManagerAccess(clubId, user);
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
        given(clubMemberRepository.findByClubIdAndUserId(clubId, managerUser.getId())).willReturn(
            java.util.Optional.of(managerMember));
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
        User memberUser = UserFixture.createUserWithId(UniversityFixture.createWithId(1), memberUserId, "일반 회원",
            "20241234", UserRole.USER);
        User targetUser = UserFixture.createUserWithId(UniversityFixture.createWithId(1), 30, "조회 대상", "20249876",
            UserRole.USER);
        User outsider = UserFixture.createUserWithId(outsiderUserId, "외부인", UserRole.USER);
        ClubMember requesterMember = ClubMemberFixture.createMember(club, memberUser);
        ClubMember targetMember = ClubMemberFixture.createMember(club, targetUser);

        given(userRepository.getById(memberUserId)).willReturn(memberUser);
        given(userRepository.getById(outsiderUserId)).willReturn(outsider);
        given(clubMemberRepository.findByClubIdAndUserId(clubId, memberUserId)).willReturn(
            java.util.Optional.of(requesterMember));
        given(clubMemberRepository.findByClubIdAndUserId(clubId, outsiderUserId)).willReturn(
            java.util.Optional.empty());
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

    @Test
    @DisplayName("getClubDetail은 회원이면 isMember=true이고 isApplied도 true이다")
    void getClubDetailReturnsMemberAndAppliedWhenUserIsMember() {
        // given
        Integer clubId = 1;
        Integer userId = 20;
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD");
        User presidentUser = UserFixture.createUserWithId(1, "회장", UserRole.USER);
        User memberUser = UserFixture.createUserWithId(userId, "회원", UserRole.USER);
        ClubMember president = ClubMemberFixture.createPresident(club, presidentUser);
        ClubMember member = ClubMemberFixture.createMember(club, memberUser);

        given(clubRepository.getById(clubId)).willReturn(club);
        given(clubMemberRepository.findAllByClubId(clubId)).willReturn(List.of(president, member));

        // when
        ClubDetailResponse response = clubService.getClubDetail(clubId, userId);

        // then
        assertThat(response.isMember()).isTrue();
        assertThat(response.isApplied()).isTrue();
        verify(clubApplyRepository, never()).existsPendingByClubIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("getClubDetail은 회원도 아니고 지원하지도 않았으면 isMember=false, isApplied=false이다")
    void getClubDetailReturnsFalseWhenUserIsNeitherMemberNorApplied() {
        // given
        Integer clubId = 1;
        Integer userId = 99;
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD");
        User presidentUser = UserFixture.createUserWithId(1, "회장", UserRole.USER);
        ClubMember president = ClubMemberFixture.createPresident(club, presidentUser);

        given(clubRepository.getById(clubId)).willReturn(club);
        given(clubMemberRepository.findAllByClubId(clubId)).willReturn(List.of(president));
        given(clubApplyRepository.existsPendingByClubIdAndUserId(clubId, userId)).willReturn(false);

        // when
        ClubDetailResponse response = clubService.getClubDetail(clubId, userId);

        // then
        assertThat(response.isMember()).isFalse();
        assertThat(response.isApplied()).isFalse();
    }

    @Test
    @DisplayName("getClubs는 지원한 동아리가 없으면 어떤 클럽도 pending으로 표시하지 않는다")
    void getClubsReturnsNoPendingWhenUserHasNoApplications() {
        // given
        Integer userId = 10;
        User user = UserFixture.createUserWithId(UniversityFixture.createWithId(1), userId, "사용자", "20240010",
            UserRole.USER);
        ClubCondition condition = new ClubCondition(1, 10, "", false);
        ClubSummaryInfo club = new ClubSummaryInfo(
            101, "동아리", "https://example.com/club.png", "학술", "설명",
            RecruitmentStatus.ONGOING, false, null
        );
        Page<ClubSummaryInfo> page = new PageImpl<>(List.of(club), PageRequest.of(0, 10), 1);

        given(userRepository.getById(userId)).willReturn(user);
        given(clubQueryRepository.findAllByFilter(PageRequest.of(0, 10), "", false, user.getUniversity().getId()))
            .willReturn(page);
        given(clubApplyRepository.findClubIdsByUserIdAndClubIdIn(userId, List.of(101)))
            .willReturn(Collections.emptyList());

        // when
        ClubsResponse response = clubService.getClubs(condition, userId);

        // then
        assertThat(response.clubs())
            .extracting(ClubsResponse.InnerClubResponse::isPendingApproval)
            .containsExactly(false);
        verify(clubMemberRepository, never()).findClubIdsByUserIdAndClubIdIn(any(), any());
    }

    @Test
    @DisplayName("getClubMembers는 부회장에게도 마스킹 없이 학번을 반환한다")
    void getClubMembersReturnsUnmaskedStudentNumbersForVicePresident() {
        // given
        Integer clubId = 1;
        User vicePresidentUser = UserFixture.createUserWithId(2, "부회장", UserRole.USER);
        User targetUser = UserFixture.createUserWithId(UniversityFixture.createWithId(1), 3, "회원", "20249876",
            UserRole.USER);
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD");
        ClubMember vicePresident = ClubMemberFixture.createVicePresident(club, vicePresidentUser);
        ClubMember targetMember = ClubMemberFixture.createMember(club, targetUser);

        given(userRepository.getById(vicePresidentUser.getId())).willReturn(vicePresidentUser);
        given(clubMemberRepository.findByClubIdAndUserId(clubId, vicePresidentUser.getId()))
            .willReturn(java.util.Optional.of(vicePresident));
        given(clubMemberRepository.findAllByClubId(clubId)).willReturn(List.of(targetMember));

        // when
        ClubMembersResponse response = clubService.getClubMembers(clubId, vicePresidentUser.getId(), null);

        // then
        assertThat(response.clubMembers())
            .extracting(ClubMembersResponse.InnerClubMember::studentNumber)
            .containsExactly(targetUser.getStudentNumber());
    }

    @Test
    @DisplayName("getClubMembers는 일반 회원이 position 필터를 사용하면 마스킹된 결과를 반환한다")
    void getClubMembersReturnsMaskedResultsForMemberWithPositionFilter() {
        // given
        Integer clubId = 1;
        Integer memberUserId = 10;
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD");
        User memberUser = UserFixture.createUserWithId(UniversityFixture.createWithId(1), memberUserId, "일반 회원",
            "20241234", UserRole.USER);
        User targetUser = UserFixture.createUserWithId(UniversityFixture.createWithId(1), 30, "조회 대상", "20249876",
            UserRole.USER);
        ClubMember requesterMember = ClubMemberFixture.createMember(club, memberUser);
        ClubMember targetMember = ClubMemberFixture.createMember(club, targetUser);
        ClubMemberCondition condition = new ClubMemberCondition(ClubPosition.MEMBER);

        given(userRepository.getById(memberUserId)).willReturn(memberUser);
        given(clubMemberRepository.findByClubIdAndUserId(clubId, memberUserId))
            .willReturn(java.util.Optional.of(requesterMember));
        given(clubMemberRepository.findAllByClubIdAndPosition(clubId, ClubPosition.MEMBER))
            .willReturn(List.of(targetMember));

        // when
        ClubMembersResponse response = clubService.getClubMembers(clubId, memberUserId, condition);

        // then
        verify(clubMemberRepository).findAllByClubIdAndPosition(clubId, ClubPosition.MEMBER);
        assertThat(response.clubMembers())
            .extracting(ClubMembersResponse.InnerClubMember::studentNumber)
            .containsExactly("*****876");
    }

    @Test
    @DisplayName("updateInfo는 매니저가 동아리 정보를 수정하면 club.updateInfo를 호출한다")
    void updateInfoCallsClubUpdateInfoForManager() {
        // given
        Integer clubId = 1;
        Integer userId = 10;
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD");
        User user = UserFixture.createUserWithId(userId, "운영진", UserRole.USER);
        ClubUpdateRequest request = new ClubUpdateRequest("새 소개", "https://new.png", "신공 201호", "새 상세 소개");

        given(userRepository.getById(userId)).willReturn(user);
        given(clubRepository.getById(clubId)).willReturn(club);

        // when
        clubService.updateInfo(clubId, userId, request);

        // then
        verify(clubPermissionValidator).validateManagerAccess(clubId, userId);
        assertThat(club.getDescription()).isEqualTo("새 소개");
        assertThat(club.getImageUrl()).isEqualTo("https://new.png");
        assertThat(club.getLocation()).isEqualTo("신공 201호");
        assertThat(club.getIntroduce()).isEqualTo("새 상세 소개");
    }

    @Test
    @DisplayName("updateInfo는 매니저 권한이 없으면 예외를 던진다")
    void updateInfoRejectsNonManagerAccess() {
        // given
        Integer clubId = 1;
        Integer userId = 10;
        User user = UserFixture.createUserWithId(userId, "일반 회원", UserRole.USER);
        ClubUpdateRequest request = new ClubUpdateRequest("새 소개", "https://new.png", "신공 201호", "새 상세 소개");

        given(userRepository.getById(userId)).willReturn(user);
        given(clubRepository.getById(clubId)).willReturn(
            ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD"));
        willThrow(CustomException.of(FORBIDDEN_ROLE_ACCESS))
            .given(clubPermissionValidator)
            .validateManagerAccess(clubId, userId);

        // when & then
        assertErrorCode(() -> clubService.updateInfo(clubId, userId, request), FORBIDDEN_ROLE_ACCESS);
    }

    @Test
    @DisplayName("updateBasicInfo는 매니저가 동아리 이름과 분과를 수정한다")
    void updateBasicInfoUpdatesNameAndCategoryForManager() {
        // given
        Integer clubId = 1;
        Integer userId = 10;
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD");
        User user = UserFixture.createUserWithId(userId, "매니저", UserRole.USER);
        ClubBasicInfoUpdateRequest request = new ClubBasicInfoUpdateRequest("새 이름", ClubCategory.SPORTS);

        given(userRepository.getById(userId)).willReturn(user);
        given(clubRepository.getById(clubId)).willReturn(club);

        // when
        clubService.updateBasicInfo(clubId, userId, request);

        // then
        verify(clubPermissionValidator).validateManagerAccess(clubId, user);
        assertThat(club.getName()).isEqualTo("새 이름");
        assertThat(club.getClubCategory()).isEqualTo(ClubCategory.SPORTS);
    }

    @Test
    @DisplayName("updateBasicInfo는 매니저 권한이 없으면 예외를 던진다")
    void updateBasicInfoRejectsNonManagerAccess() {
        // given
        Integer clubId = 1;
        Integer userId = 10;
        User user = UserFixture.createUserWithId(userId, "일반 회원", UserRole.USER);
        ClubBasicInfoUpdateRequest request = new ClubBasicInfoUpdateRequest("새 이름", ClubCategory.SPORTS);

        given(userRepository.getById(userId)).willReturn(user);
        given(clubRepository.getById(clubId)).willReturn(
            ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD"));
        willThrow(CustomException.of(ApiResponseCode.FORBIDDEN_CLUB_MANAGER_ACCESS))
            .given(clubPermissionValidator)
            .validateManagerAccess(clubId, user);

        // when & then
        assertErrorCode(
            () -> clubService.updateBasicInfo(clubId, userId, request),
            ApiResponseCode.FORBIDDEN_CLUB_MANAGER_ACCESS
        );
    }

    @Test
    @DisplayName("getJoinedClubs는 사용자가 가입한 동아리 목록을 반환한다")
    void getJoinedClubsReturnsUsersClubMemberships() {
        // given
        Integer userId = 10;
        User user = UserFixture.createUserWithId(userId, "사용자", UserRole.USER);
        Club club1 = ClubFixture.createWithId(UniversityFixture.createWithId(1), 100, "동아리1");
        Club club2 = ClubFixture.createWithId(UniversityFixture.createWithId(1), 200, "동아리2");
        ClubMember member1 = ClubMemberFixture.createMember(club1, user);
        ClubMember member2 = ClubMemberFixture.createManager(club2, user);

        given(clubMemberRepository.findAllByUserId(userId)).willReturn(List.of(member1, member2));

        // when
        ClubMembershipsResponse response = clubService.getJoinedClubs(userId);

        // then
        assertThat(response.joinedClubs()).hasSize(2);
        assertThat(response.joinedClubs())
            .extracting(ClubMembershipsResponse.InnerJoinedClubResponse::id)
            .containsExactly(100, 200);
    }

    @Test
    @DisplayName("getJoinedClubs는 가입한 동아리가 없으면 빈 목록을 반환한다")
    void getJoinedClubsReturnsEmptyListWhenNoClubsJoined() {
        // given
        Integer userId = 10;
        given(clubMemberRepository.findAllByUserId(userId)).willReturn(Collections.emptyList());

        // when
        ClubMembershipsResponse response = clubService.getJoinedClubs(userId);

        // then
        assertThat(response.joinedClubs()).isEmpty();
    }

    @Test
    @DisplayName("getManagedClubDetail은 관리자에게 forAdmin 응답을 반환한다")
    void getManagedClubDetailReturnsForAdminResponseForAdmin() {
        // given
        Integer clubId = 1;
        Integer adminId = 10;
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD");
        User admin = UserFixture.createUserWithId(adminId, "관리자", UserRole.ADMIN);

        given(clubRepository.getById(clubId)).willReturn(club);
        given(userRepository.getById(adminId)).willReturn(admin);

        // when
        MyManagedClubResponse response = clubService.getManagedClubDetail(clubId, adminId);

        // then
        verify(clubPermissionValidator, never()).validateManagerAccess(any(Integer.class), any(Integer.class));
        assertThat(response.clubId()).isEqualTo(clubId);
        assertThat(response.position()).isEqualTo(ClubPosition.PRESIDENT.getDescription());
        assertThat(response.name()).isEqualTo(admin.getName());
    }

    @Test
    @DisplayName("getManagedClubs는 매니저 권한이 없는 일반 회원에게 빈 목록을 반환한다")
    void getManagedClubsReturnsEmptyForRegularMemberWithNoManagerPositions() {
        // given
        Integer userId = 10;
        User user = UserFixture.createUserWithId(userId, "일반 회원", UserRole.USER);

        given(userRepository.getById(userId)).willReturn(user);
        given(clubMemberRepository.findAllByUserIdAndClubPositions(userId, ClubPosition.MANAGERS))
            .willReturn(Collections.emptyList());

        // when
        ClubMembershipsResponse response = clubService.getManagedClubs(userId);

        // then
        assertThat(response.joinedClubs()).isEmpty();
    }

    @Test
    @DisplayName("getClubDetail은 회장이 존재하지 않으면 NOT_FOUND_CLUB_PRESIDENT 예외를 던진다")
    void getClubDetailThrowsWhenNoPresidentExists() {
        // given
        Integer clubId = 1;
        Integer userId = 99;
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD");
        User memberUser = UserFixture.createUserWithId(10, "일반 회원", UserRole.USER);
        ClubMember memberOnly = ClubMemberFixture.createMember(club, memberUser);

        given(clubRepository.getById(clubId)).willReturn(club);
        given(clubMemberRepository.findAllByClubId(clubId)).willReturn(List.of(memberOnly));

        // when & then
        assertErrorCode(
            () -> clubService.getClubDetail(clubId, userId),
            ApiResponseCode.NOT_FOUND_CLUB_PRESIDENT
        );
    }

    @Test
    @DisplayName("getClubMembers는 condition이 있지만 position이 null이면 전체 회원을 조회한다")
    void getClubMembersFetchesAllMembersWhenConditionHasNullPosition() {
        // given
        Integer clubId = 1;
        Integer userId = 10;
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD");
        User user = UserFixture.createUserWithId(userId, "운영진", UserRole.USER);
        User targetUser = UserFixture.createUserWithId(UniversityFixture.createWithId(1), 20, "회원", "20249876",
            UserRole.USER);
        ClubMember requester = ClubMemberFixture.createManager(club, user);
        ClubMember targetMember = ClubMemberFixture.createMember(club, targetUser);
        ClubMemberCondition condition = new ClubMemberCondition(null);

        given(userRepository.getById(userId)).willReturn(user);
        given(clubMemberRepository.findByClubIdAndUserId(clubId, userId)).willReturn(java.util.Optional.of(requester));
        given(clubMemberRepository.findAllByClubId(clubId)).willReturn(List.of(requester, targetMember));

        // when
        ClubMembersResponse response = clubService.getClubMembers(clubId, userId, condition);

        // then
        verify(clubMemberRepository).findAllByClubId(clubId);
        verify(clubMemberRepository, never()).findAllByClubIdAndPosition(any(), any());
        assertThat(response.clubMembers()).hasSize(2);
    }

    @Test
    @DisplayName("getClubMembers는 회원이 없으면 빈 목록을 반환한다")
    void getClubMembersReturnsEmptyListWhenNoMembers() {
        // given
        Integer clubId = 1;
        Integer userId = 10;
        User admin = UserFixture.createUserWithId(userId, "관리자", UserRole.ADMIN);

        given(userRepository.getById(userId)).willReturn(admin);
        given(clubMemberRepository.findAllByClubId(clubId)).willReturn(Collections.emptyList());

        // when
        ClubMembersResponse response = clubService.getClubMembers(clubId, userId, null);

        // then
        assertThat(response.clubMembers()).isEmpty();
    }

    @Test
    @DisplayName("getManagedClubDetail은 매니저가 아닌 일반 사용자의 접근을 거부한다")
    void getManagedClubDetailRejectsNonManagerNonAdminUser() {
        // given
        Integer clubId = 1;
        Integer userId = 10;
        User user = UserFixture.createUserWithId(userId, "일반 사용자", UserRole.USER);
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD");

        given(clubRepository.getById(clubId)).willReturn(club);
        given(userRepository.getById(userId)).willReturn(user);
        willThrow(CustomException.of(FORBIDDEN_ROLE_ACCESS))
            .given(clubPermissionValidator)
            .validateManagerAccess(clubId, user);

        // when & then
        assertErrorCode(() -> clubService.getManagedClubDetail(clubId, userId), FORBIDDEN_ROLE_ACCESS);
        verify(clubMemberRepository, never()).getByClubIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("getClubs는 id가 null인 ClubSummaryInfo를 pending 계산에서 제외한다")
    void getClubsSkipsNullIdClubSummariesInPendingCalculation() {
        // given
        Integer userId = 10;
        User user = UserFixture.createUserWithId(UniversityFixture.createWithId(1), userId, "사용자", "20240010",
            UserRole.USER);
        ClubCondition condition = new ClubCondition(1, 10, "", false);
        ClubSummaryInfo nullIdClub = new ClubSummaryInfo(
            null, "null id 동아리", "https://example.com/null.png", "학술", "설명",
            RecruitmentStatus.ONGOING, false, null
        );
        ClubSummaryInfo validClub = new ClubSummaryInfo(
            200, "정상 동아리", "https://example.com/valid.png", "학술", "설명",
            RecruitmentStatus.ONGOING, false, null
        );
        Page<ClubSummaryInfo> page = new PageImpl<>(List.of(nullIdClub, validClub), PageRequest.of(0, 10), 2);

        given(userRepository.getById(userId)).willReturn(user);
        given(clubQueryRepository.findAllByFilter(PageRequest.of(0, 10), "", false, user.getUniversity().getId()))
            .willReturn(page);
        // null id는 필터링되므로 clubIds에는 200만 남는다
        given(clubApplyRepository.findClubIdsByUserIdAndClubIdIn(userId, List.of(200)))
            .willReturn(List.of(200));
        given(clubMemberRepository.findClubIdsByUserIdAndClubIdIn(userId, List.of(200)))
            .willReturn(Collections.emptyList());

        // when
        ClubsResponse response = clubService.getClubs(condition, userId);

        // then
        // null id 클럽은 pending 계산에서 제외되었으므로 repository에는 [200]만 전달됨
        verify(clubApplyRepository).findClubIdsByUserIdAndClubIdIn(userId, List.of(200));
        assertThat(response.clubs())
            .extracting(ClubsResponse.InnerClubResponse::id, ClubsResponse.InnerClubResponse::isPendingApproval)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(null, false),
                org.assertj.core.groups.Tuple.tuple(200, true)
            );
    }

    @Test
    @DisplayName("getClubMembers는 학번이 3자 이하이면 마스킹 없이 그대로 반환한다")
    void getClubMembersDoesNotMaskShortStudentNumber() {
        // given
        Integer clubId = 1;
        Integer memberUserId = 10;
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD");
        User memberUser = UserFixture.createUserWithId(UniversityFixture.createWithId(1), memberUserId, "회원",
            "20241234", UserRole.USER);
        User shortIdUser = UserFixture.createUserWithId(UniversityFixture.createWithId(1), 30, "짧은학번", "123",
            UserRole.USER);
        ClubMember requester = ClubMemberFixture.createMember(club, memberUser);
        ClubMember shortIdMember = ClubMemberFixture.createMember(club, shortIdUser);

        given(userRepository.getById(memberUserId)).willReturn(memberUser);
        given(clubMemberRepository.findByClubIdAndUserId(clubId, memberUserId))
            .willReturn(java.util.Optional.of(requester));
        given(clubMemberRepository.findAllByClubId(clubId)).willReturn(List.of(requester, shortIdMember));

        // when
        ClubMembersResponse response = clubService.getClubMembers(clubId, memberUserId, null);

        // then
        assertThat(response.clubMembers())
            .extracting(ClubMembersResponse.InnerClubMember::studentNumber)
            .containsExactly("*****234", "123");
    }

    @Test
    @DisplayName("getClubMembers는 학번이 null이면 null을 그대로 반환한다")
    void getClubMembersReturnsNullWhenStudentNumberIsNull() {
        // given
        Integer clubId = 1;
        Integer memberUserId = 10;
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD");
        User memberUser = UserFixture.createUserWithId(UniversityFixture.createWithId(1), memberUserId, "회원",
            "20241234", UserRole.USER);
        User nullStudentNumberUser = UserFixture.createUserWithId(UniversityFixture.createWithId(1), 30, "학번없음", null,
            UserRole.USER);
        ClubMember requester = ClubMemberFixture.createMember(club, memberUser);
        ClubMember nullStudentNumberMember = ClubMemberFixture.createMember(club, nullStudentNumberUser);

        given(userRepository.getById(memberUserId)).willReturn(memberUser);
        given(clubMemberRepository.findByClubIdAndUserId(clubId, memberUserId))
            .willReturn(java.util.Optional.of(requester));
        given(clubMemberRepository.findAllByClubId(clubId)).willReturn(List.of(requester, nullStudentNumberMember));

        // when
        ClubMembersResponse response = clubService.getClubMembers(clubId, memberUserId, null);

        // then
        assertThat(response.clubMembers())
            .extracting(ClubMembersResponse.InnerClubMember::studentNumber)
            .containsExactly("*****234", null);
    }

    @Test
    @DisplayName("getClubs는 모든 지원 동아리가 가입 상태이면 pending 집합이 비어있다")
    void getClubsReturnsEmptyPendingWhenAllAppliedClubsAreAlsoJoined() {
        // given
        Integer userId = 10;
        User user = UserFixture.createUserWithId(UniversityFixture.createWithId(1), userId, "사용자", "20240010",
            UserRole.USER);
        ClubCondition condition = new ClubCondition(1, 10, "", false);
        ClubSummaryInfo club1 = new ClubSummaryInfo(
            101, "동아리1", "https://example.com/1.png", "학술", "설명",
            RecruitmentStatus.ONGOING, false, null
        );
        ClubSummaryInfo club2 = new ClubSummaryInfo(
            102, "동아리2", "https://example.com/2.png", "학술", "설명",
            RecruitmentStatus.ONGOING, false, null
        );
        Page<ClubSummaryInfo> page = new PageImpl<>(List.of(club1, club2), PageRequest.of(0, 10), 2);

        given(userRepository.getById(userId)).willReturn(user);
        given(clubQueryRepository.findAllByFilter(PageRequest.of(0, 10), "", false, user.getUniversity().getId()))
            .willReturn(page);
        // 두 클럽 모두에 지원했고, 두 클럽 모두 가입 상태
        given(clubApplyRepository.findClubIdsByUserIdAndClubIdIn(userId, List.of(101, 102)))
            .willReturn(List.of(101, 102));
        given(clubMemberRepository.findClubIdsByUserIdAndClubIdIn(userId, List.of(101, 102)))
            .willReturn(List.of(101, 102));

        // when
        ClubsResponse response = clubService.getClubs(condition, userId);

        // then
        assertThat(response.clubs())
            .extracting(ClubsResponse.InnerClubResponse::isPendingApproval)
            .containsExactly(false, false);
    }

    private void assertErrorCode(ThrowingCallable callable, ApiResponseCode errorCode) {
        assertThatThrownBy(callable)
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode()).isEqualTo(errorCode));
    }
}
