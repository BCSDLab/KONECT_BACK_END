package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import gg.agit.konect.domain.club.dto.ClubCondition;
import gg.agit.konect.domain.club.dto.ClubCreateRequest;
import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubMemberCondition;
import gg.agit.konect.domain.club.dto.ClubMembersResponse;
import gg.agit.konect.domain.club.dto.ClubMembershipsResponse;
import gg.agit.konect.domain.club.dto.ClubUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubsResponse;
import gg.agit.konect.domain.club.dto.MyManagedClubResponse;
import gg.agit.konect.domain.club.dto.ClubBasicInfoUpdateRequest;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.enums.RecruitmentStatus;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.model.ClubSummaryInfo;
import gg.agit.konect.domain.club.repository.ClubApplyRepository;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubQueryRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.university.enums.Campus;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClubService 단위 테스트")
class ClubServiceTest {

    private static final int CLUB_ID = 11;
    private static final int USER_ID = 22;
    private static final int OTHER_USER_ID = 23;
    private static final int UNIVERSITY_ID = 5;
    private static final int PAGE = 1;
    private static final int LIMIT = 10;
    private static final int FIRST_CLUB_ID = 1001;
    private static final int SECOND_CLUB_ID = 1002;
    private static final int TOTAL_CLUB_COUNT = 2;
    private static final int PAGE_INDEX = 0;
    private static final int FIRST_INDEX = 0;
    private static final int SECOND_INDEX = 1;
    private static final int YEAR = 2026;
    private static final int MONTH_FEBRUARY = 2;
    private static final int START_DAY = 1;
    private static final int END_DAY = 28;
    private static final int CREATED_CLUB_ID = 77;
    private static final int MANAGER_USER_ID = 24;

    @Mock
    private ClubQueryRepository clubQueryRepository;
    @Mock
    private ClubRepository clubRepository;
    @Mock
    private ClubMemberRepository clubMemberRepository;
    @Mock
    private ClubApplyRepository clubApplyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClubPermissionValidator clubPermissionValidator;

    @InjectMocks
    private ClubService clubService;

    @Nested
    @DisplayName("getClubMembers 테스트")
    class GetClubMembersTests {

        @Test
        @DisplayName("비회원이 조회하면 FORBIDDEN_CLUB_MEMBER_ACCESS 예외가 발생한다")
        void getClubMembersForNonMemberThrowsCustomException() {
            // Given
            when(clubMemberRepository.existsByClubIdAndUserId(CLUB_ID, USER_ID)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> clubService.getClubMembers(CLUB_ID, USER_ID, null))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(ex.getErrorCode())
                    .isEqualTo(ApiResponseCode.FORBIDDEN_CLUB_MEMBER_ACCESS));
        }

        @Test
        @DisplayName("직책 조건이 있으면 해당 직책 멤버 목록으로 조회한다")
        void getClubMembersWithPositionConditionUsesFilteredRepositoryCall() {
            // Given
            ClubMember president = createMember(CLUB_ID, USER_ID, ClubPosition.PRESIDENT, "회장");
            when(clubMemberRepository.existsByClubIdAndUserId(CLUB_ID, USER_ID)).thenReturn(true);
            when(clubMemberRepository.findAllByClubIdAndPosition(CLUB_ID, ClubPosition.PRESIDENT))
                .thenReturn(List.of(president));

            // When
            ClubMembersResponse result = clubService.getClubMembers(
                CLUB_ID,
                USER_ID,
                new ClubMemberCondition(ClubPosition.PRESIDENT)
            );

            // Then
            assertThat(result.clubMembers()).hasSize(1);
            verify(clubMemberRepository).findAllByClubIdAndPosition(CLUB_ID, ClubPosition.PRESIDENT);
        }

        @Test
        @DisplayName("조건이 null이면 전체 멤버 목록으로 조회한다")
        void getClubMembersWithNullConditionUsesAllMembersQuery() {
            // Given
            ClubMember member = createMember(CLUB_ID, USER_ID, ClubPosition.MEMBER, "회원");
            when(clubMemberRepository.existsByClubIdAndUserId(CLUB_ID, USER_ID)).thenReturn(true);
            when(clubMemberRepository.findAllByClubId(CLUB_ID)).thenReturn(List.of(member));

            // When
            ClubMembersResponse result = clubService.getClubMembers(CLUB_ID, USER_ID, null);

            // Then
            assertThat(result.clubMembers()).hasSize(1);
            verify(clubMemberRepository).findAllByClubId(CLUB_ID);
        }

        @Test
        @DisplayName("condition.position이 null이면 전체 멤버 목록으로 조회한다")
        void getClubMembersWithNullPositionUsesAllMembersQuery() {
            // Given
            ClubMember member = createMember(CLUB_ID, USER_ID, ClubPosition.MEMBER, "회원");
            when(clubMemberRepository.existsByClubIdAndUserId(CLUB_ID, USER_ID)).thenReturn(true);
            when(clubMemberRepository.findAllByClubId(CLUB_ID)).thenReturn(List.of(member));

            // When
            ClubMembersResponse result = clubService.getClubMembers(
                CLUB_ID,
                USER_ID,
                new ClubMemberCondition(null)
            );

            // Then
            assertThat(result.clubMembers()).hasSize(1);
            verify(clubMemberRepository).findAllByClubId(CLUB_ID);
        }
    }

    @Nested
    @DisplayName("getClubDetail 테스트")
    class GetClubDetailTests {

        @Test
        @DisplayName("이미 멤버이면 isMember=true, isApplied=true를 반환한다")
        void getClubDetailForMemberReturnsMemberAndAppliedTrue() {
            // Given
            Club club = createClub(CLUB_ID);
            ClubRecruitment recruitment = ClubRecruitment.builder()
                .club(club)
                .isAlwaysRecruiting(false)
                .startDate(LocalDate.of(YEAR, MONTH_FEBRUARY, START_DAY))
                .endDate(LocalDate.of(YEAR, MONTH_FEBRUARY, END_DAY))
                .content("모집")
                .build();
            Club clubWithRecruitment = Club.builder()
                .id(club.getId())
                .name(club.getName())
                .description(club.getDescription())
                .introduce(club.getIntroduce())
                .imageUrl(club.getImageUrl())
                .location(club.getLocation())
                .clubCategory(club.getClubCategory())
                .university(club.getUniversity())
                .clubRecruitment(recruitment)
                .build();

            ClubMember president = createMember(CLUB_ID, USER_ID, ClubPosition.PRESIDENT, "회장");

            when(clubRepository.getById(CLUB_ID)).thenReturn(clubWithRecruitment);
            when(clubMemberRepository.findAllByClubId(CLUB_ID)).thenReturn(List.of(president));

            // When
            ClubDetailResponse result = clubService.getClubDetail(CLUB_ID, USER_ID);

            // Then
            assertThat(result.isMember()).isTrue();
            assertThat(result.isApplied()).isTrue();
            assertThat(result.presidentName()).isEqualTo("회장");
        }

        @Test
        @DisplayName("비회원이지만 지원했으면 isMember=false, isApplied=true를 반환한다")
        void getClubDetailForAppliedNonMemberReturnsAppliedTrue() {
            // Given
            Club club = createClub(CLUB_ID);
            ClubRecruitment recruitment = ClubRecruitment.builder()
                .club(club)
                .isAlwaysRecruiting(true)
                .content("상시 모집")
                .build();
            Club clubWithRecruitment = Club.builder()
                .id(club.getId())
                .name(club.getName())
                .description(club.getDescription())
                .introduce(club.getIntroduce())
                .imageUrl(club.getImageUrl())
                .location(club.getLocation())
                .clubCategory(club.getClubCategory())
                .university(club.getUniversity())
                .clubRecruitment(recruitment)
                .build();

            ClubMember president = createMember(CLUB_ID, USER_ID, ClubPosition.PRESIDENT, "회장");

            when(clubRepository.getById(CLUB_ID)).thenReturn(clubWithRecruitment);
            when(clubMemberRepository.findAllByClubId(CLUB_ID)).thenReturn(List.of(president));
            when(clubApplyRepository.existsByClubIdAndUserId(CLUB_ID, OTHER_USER_ID)).thenReturn(true);

            // When
            ClubDetailResponse result = clubService.getClubDetail(CLUB_ID, OTHER_USER_ID);

            // Then
            assertThat(result.isMember()).isFalse();
            assertThat(result.isApplied()).isTrue();
            assertThat(result.recruitment().status()).isEqualTo(RecruitmentStatus.ONGOING);
        }

        @Test
        @DisplayName("비회원이고 지원하지 않았으면 isApplied=false를 반환한다")
        void getClubDetailForNotAppliedNonMemberReturnsAppliedFalse() {
            // Given
            Club club = createClub(CLUB_ID);
            ClubMember president = createMember(CLUB_ID, USER_ID, ClubPosition.PRESIDENT, "회장");

            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(clubMemberRepository.findAllByClubId(CLUB_ID)).thenReturn(List.of(president));
            when(clubApplyRepository.existsByClubIdAndUserId(CLUB_ID, OTHER_USER_ID)).thenReturn(false);

            // When
            ClubDetailResponse result = clubService.getClubDetail(CLUB_ID, OTHER_USER_ID);

            // Then
            assertThat(result.isMember()).isFalse();
            assertThat(result.isApplied()).isFalse();
        }
    }

    @Nested
    @DisplayName("getClubs 테스트")
    class GetClubsTests {

        @Test
        @DisplayName("이미 가입한 동아리는 pendingApproval에서 제외한다")
        void getClubsRemovesJoinedClubFromPendingApprovalSet() {
            // Given
            University university = University.builder()
                .id(UNIVERSITY_ID)
                .koreanName("경북대")
                .campus(Campus.MAIN)
                .build();
            User user = User.builder()
                .id(USER_ID)
                .name("조회자")
                .email("member@konect.gg")
                .studentNumber("2020123456")
                .provider(Provider.GOOGLE)
                .providerId("pid")
                .university(university)
                .build();

            ClubCondition condition = new ClubCondition(PAGE, LIMIT, "", false);
            ClubSummaryInfo first = new ClubSummaryInfo(
                FIRST_CLUB_ID,
                "A",
                "img",
                "학술",
                "desc",
                RecruitmentStatus.ONGOING,
                false,
                null
            );
            ClubSummaryInfo second = new ClubSummaryInfo(
                SECOND_CLUB_ID,
                "B",
                "img",
                "학술",
                "desc",
                RecruitmentStatus.ONGOING,
                false,
                null
            );
            Page<ClubSummaryInfo> page = new PageImpl<>(
                List.of(first, second),
                PageRequest.of(PAGE_INDEX, LIMIT),
                TOTAL_CLUB_COUNT
            );

            when(userRepository.getById(USER_ID)).thenReturn(user);
            when(clubQueryRepository.findAllByFilter(any(PageRequest.class), eq(""), eq(false), eq(UNIVERSITY_ID)))
                .thenReturn(page);
            when(clubApplyRepository.findClubIdsByUserIdAndClubIdIn(USER_ID, List.of(FIRST_CLUB_ID, SECOND_CLUB_ID)))
                .thenReturn(List.of(FIRST_CLUB_ID, SECOND_CLUB_ID));
            when(clubMemberRepository.findClubIdsByUserIdAndClubIdIn(USER_ID, List.of(FIRST_CLUB_ID, SECOND_CLUB_ID)))
                .thenReturn(List.of(FIRST_CLUB_ID));

            // When
            ClubsResponse response = clubService.getClubs(condition, USER_ID);

            // Then
            assertThat(response.clubs()).hasSize(TOTAL_CLUB_COUNT);
            assertThat(response.clubs().get(FIRST_INDEX).isPendingApproval()).isFalse();
            assertThat(response.clubs().get(SECOND_INDEX).isPendingApproval()).isTrue();
        }

        @Test
        @DisplayName("조회 결과가 비어있으면 지원/가입 조회 없이 빈 목록을 반환한다")
        void getClubsWithEmptyPageSkipsPendingCalculationQueries() {
            // Given
            ClubCondition condition = new ClubCondition(PAGE, LIMIT, "", false);
            User user = createUser(USER_ID, "조회자");
            Page<ClubSummaryInfo> emptyPage = new PageImpl<>(List.of(), PageRequest.of(PAGE_INDEX, LIMIT), 0);

            when(userRepository.getById(USER_ID)).thenReturn(user);
            when(clubQueryRepository.findAllByFilter(any(PageRequest.class), eq(""), eq(false), eq(UNIVERSITY_ID)))
                .thenReturn(emptyPage);

            // When
            ClubsResponse response = clubService.getClubs(condition, USER_ID);

            // Then
            assertThat(response.clubs()).isEmpty();
            verify(clubApplyRepository, never()).findClubIdsByUserIdAndClubIdIn(any(), any());
            verify(clubMemberRepository, never()).findClubIdsByUserIdAndClubIdIn(any(), any());
        }

        @Test
        @DisplayName("지원한 동아리가 없으면 가입 조회 없이 pendingApproval=false로 반환한다")
        void getClubsWithNoAppliedClubSkipsMemberClubLookup() {
            // Given
            ClubCondition condition = new ClubCondition(PAGE, LIMIT, "", false);
            User user = createUser(USER_ID, "조회자");
            ClubSummaryInfo first = new ClubSummaryInfo(
                FIRST_CLUB_ID,
                "A",
                "img",
                "학술",
                "desc",
                RecruitmentStatus.ONGOING,
                false,
                null
            );
            Page<ClubSummaryInfo> page = new PageImpl<>(List.of(first), PageRequest.of(PAGE_INDEX, LIMIT), 1);

            when(userRepository.getById(USER_ID)).thenReturn(user);
            when(clubQueryRepository.findAllByFilter(any(PageRequest.class), eq(""), eq(false), eq(UNIVERSITY_ID)))
                .thenReturn(page);
            when(clubApplyRepository.findClubIdsByUserIdAndClubIdIn(USER_ID, List.of(FIRST_CLUB_ID)))
                .thenReturn(List.of());

            // When
            ClubsResponse response = clubService.getClubs(condition, USER_ID);

            // Then
            assertThat(response.clubs()).hasSize(1);
            assertThat(response.clubs().get(FIRST_INDEX).isPendingApproval()).isFalse();
            verify(clubMemberRepository, never()).findClubIdsByUserIdAndClubIdIn(any(), any());
        }
    }

    @Nested
    @DisplayName("createClub 테스트")
    class CreateClubTests {

        @Test
        @DisplayName("동아리 생성 시 회장을 저장하고 상세 응답을 반환한다")
        void createClubCreatesPresidentAndReturnsDetail() {
            // Given
            User user = createUser(USER_ID, "생성자");
            ClubCreateRequest request = new ClubCreateRequest(
                "새동아리",
                "한줄소개",
                "상세소개",
                "https://new-image",
                "학생회관",
                ClubCategory.ACADEMIC
            );
            Club savedClub = createClub(CREATED_CLUB_ID);
            ClubMember president = createMember(CREATED_CLUB_ID, USER_ID, ClubPosition.PRESIDENT, "생성자");

            when(userRepository.getById(USER_ID)).thenReturn(user);
            when(clubRepository.save(any(Club.class))).thenReturn(savedClub);
            when(clubRepository.getById(CREATED_CLUB_ID)).thenReturn(savedClub);
            when(clubMemberRepository.findAllByClubId(CREATED_CLUB_ID)).thenReturn(List.of(president));

            // When
            ClubDetailResponse result = clubService.createClub(USER_ID, request);

            // Then
            assertThat(result.id()).isEqualTo(CREATED_CLUB_ID);
            assertThat(result.isMember()).isTrue();
            verify(clubMemberRepository).save(any(ClubMember.class));
        }
    }

    @Nested
    @DisplayName("updateInfo 테스트")
    class UpdateInfoTests {

        @Test
        @DisplayName("관리 권한이 있으면 정보가 갱신된다")
        void updateInfoWithManagerAccessUpdatesClubInfo() {
            // Given
            Club club = createClub(CLUB_ID);
            ClubUpdateRequest request = new ClubUpdateRequest("변경소개", "https://updated", "새위치", "새상세");

            when(userRepository.getById(USER_ID)).thenReturn(createUser(USER_ID, "관리자"));
            when(clubRepository.getById(CLUB_ID)).thenReturn(club);

            // When
            clubService.updateInfo(CLUB_ID, USER_ID, request);

            // Then
            assertThat(club.getDescription()).isEqualTo("변경소개");
            assertThat(club.getImageUrl()).isEqualTo("https://updated");
            assertThat(club.getLocation()).isEqualTo("새위치");
            assertThat(club.getIntroduce()).isEqualTo("새상세");
            verify(clubPermissionValidator).validateManagerAccess(CLUB_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("updateBasicInfo 테스트")
    class UpdateBasicInfoTests {

        @Test
        @DisplayName("기본 정보를 갱신한다")
        void updateBasicInfoUpdatesClubNameAndCategory() {
            // Given
            Club club = createClub(CLUB_ID);
            ClubBasicInfoUpdateRequest request = new ClubBasicInfoUpdateRequest("새이름", ClubCategory.SPORTS);

            when(userRepository.getById(USER_ID)).thenReturn(createUser(USER_ID, "관리자"));
            when(clubRepository.getById(CLUB_ID)).thenReturn(club);

            // When
            clubService.updateBasicInfo(CLUB_ID, USER_ID, request);

            // Then
            assertThat(club.getName()).isEqualTo("새이름");
            assertThat(club.getClubCategory()).isEqualTo(ClubCategory.SPORTS);
            verifyNoInteractions(clubPermissionValidator);
        }
    }

    @Nested
    @DisplayName("membership 조회 테스트")
    class MembershipReadTests {

        @Test
        @DisplayName("getJoinedClubs는 사용자의 가입 동아리 목록을 반환한다")
        void getJoinedClubsReturnsJoinedClubMemberships() {
            // Given
            ClubMember joined = createMember(CLUB_ID, USER_ID, ClubPosition.MEMBER, "회원");
            when(clubMemberRepository.findAllByUserId(USER_ID)).thenReturn(List.of(joined));

            // When
            ClubMembershipsResponse response = clubService.getJoinedClubs(USER_ID);

            // Then
            assertThat(response.joinedClubs()).hasSize(1);
            assertThat(response.joinedClubs().get(FIRST_INDEX).id()).isEqualTo(CLUB_ID);
        }

        @Test
        @DisplayName("getManagedClubs는 운영진 권한 동아리 목록을 반환한다")
        void getManagedClubsReturnsManagedClubMemberships() {
            // Given
            ClubMember managed = createMember(CLUB_ID, MANAGER_USER_ID, ClubPosition.MANAGER, "운영진");
            when(clubMemberRepository.findAllByUserIdAndClubPositions(eq(MANAGER_USER_ID), any()))
                .thenReturn(List.of(managed));

            // When
            ClubMembershipsResponse response = clubService.getManagedClubs(MANAGER_USER_ID);

            // Then
            assertThat(response.joinedClubs()).hasSize(1);
            assertThat(response.joinedClubs().get(FIRST_INDEX).position())
                .isEqualTo(ClubPosition.MANAGER.getDescription());
        }
    }

    @Nested
    @DisplayName("getManagedClubDetail 테스트")
    class GetManagedClubDetailTests {

        @Test
        @DisplayName("관리 권한이 있으면 관리 상세 응답을 반환한다")
        void getManagedClubDetailWithManagerAccessReturnsDetail() {
            // Given
            Club club = createClub(CLUB_ID);
            ClubMember manager = createMember(CLUB_ID, MANAGER_USER_ID, ClubPosition.MANAGER, "운영진");

            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, MANAGER_USER_ID)).thenReturn(manager);

            // When
            MyManagedClubResponse response = clubService.getManagedClubDetail(CLUB_ID, MANAGER_USER_ID);

            // Then
            assertThat(response.clubId()).isEqualTo(CLUB_ID);
            assertThat(response.position()).isEqualTo(ClubPosition.MANAGER.getDescription());
            verify(clubPermissionValidator).validateManagerAccess(CLUB_ID, MANAGER_USER_ID);
        }
    }

    private Club createClub(Integer clubId) {
        return Club.builder()
            .id(clubId)
            .name("BCSD")
            .description("설명")
            .introduce("소개")
            .imageUrl("https://img")
            .location("학생회관")
            .clubCategory(ClubCategory.ACADEMIC)
            .university(University.builder().id(UNIVERSITY_ID).koreanName("경북대").campus(Campus.MAIN).build())
            .build();
    }

    private ClubMember createMember(Integer clubId, Integer userId, ClubPosition position, String name) {
        Club club = createClub(clubId);
        User user = User.builder()
            .id(userId)
            .name(name)
            .email(name + "@konect.gg")
            .studentNumber("2020123456")
            .provider(Provider.GOOGLE)
            .providerId("pid-" + userId)
            .university(University.builder().id(UNIVERSITY_ID).koreanName("경북대").campus(Campus.MAIN).build())
            .build();

        return ClubMember.builder()
            .club(club)
            .user(user)
            .clubPosition(position)
            .isFeePaid(true)
            .build();
    }

    private User createUser(Integer userId, String name) {
        return User.builder()
            .id(userId)
            .name(name)
            .email(name + "@konect.gg")
            .studentNumber("2020123456")
            .provider(Provider.GOOGLE)
            .providerId("pid-" + userId)
            .university(University.builder().id(UNIVERSITY_ID).koreanName("경북대").campus(Campus.MAIN).build())
            .build();
    }

}
