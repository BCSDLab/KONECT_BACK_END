package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gg.agit.konect.domain.club.dto.ClubPreMemberAddRequest;
import gg.agit.konect.domain.club.dto.MemberPositionChangeRequest;
import gg.agit.konect.domain.club.dto.PresidentTransferRequest;
import gg.agit.konect.domain.club.dto.VicePresidentChangeRequest;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubPreMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.university.enums.Campus;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClubMemberManagementService 단위 테스트")
class ClubMemberManagementServiceTest {

    private static final int CLUB_ID = 10;
    private static final int REQUESTER_ID = 20;
    private static final int TARGET_ID = 21;
    private static final int NEW_LEADER_ID = 22;
    private static final int OTHER_ID = 23;
    private static final int UNIVERSITY_ID = 1;
    private static final String STUDENT_NUMBER = "2020123456";

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private ClubPreMemberRepository clubPreMemberRepository;

    @Mock
    private ClubPermissionValidator clubPermissionValidator;

    @InjectMocks
    private ClubMemberManagementService clubMemberManagementService;

    @Nested
    @DisplayName("changeMemberPosition 테스트")
    class ChangeMemberPositionTests {

        @Test
        @DisplayName("자기 자신의 직책 변경은 CANNOT_CHANGE_OWN_POSITION 예외가 발생한다")
        void changeMemberPositionWithSelfTargetThrowsCustomException() {
            // Given
            MemberPositionChangeRequest request = new MemberPositionChangeRequest(ClubPosition.MEMBER);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));

            // When & Then
            assertThatThrownBy(() -> clubMemberManagementService.changeMemberPosition(
                CLUB_ID,
                REQUESTER_ID,
                REQUESTER_ID,
                request
            )).isInstanceOfSatisfying(CustomException.class, ex -> assertThat(
                ex.getErrorCode()).isEqualTo(ApiResponseCode.CANNOT_CHANGE_OWN_POSITION));

            verifyNoInteractions(clubPermissionValidator);
        }

        @Test
        @DisplayName("요청자가 대상보다 낮은 권한이면 CANNOT_MANAGE_HIGHER_POSITION 예외가 발생한다")
        void changeMemberPositionWithHigherTargetThrowsCustomException() {
            // Given
            MemberPositionChangeRequest request = new MemberPositionChangeRequest(ClubPosition.MEMBER);
            ClubMember requester = createMember(CLUB_ID, REQUESTER_ID, ClubPosition.MANAGER);
            ClubMember target = createMember(CLUB_ID, TARGET_ID, ClubPosition.VICE_PRESIDENT);

            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, REQUESTER_ID)).thenReturn(requester);
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, TARGET_ID)).thenReturn(target);

            // When & Then
            assertThatThrownBy(() -> clubMemberManagementService.changeMemberPosition(
                CLUB_ID,
                TARGET_ID,
                REQUESTER_ID,
                request
            )).isInstanceOfSatisfying(CustomException.class, ex -> assertThat(
                ex.getErrorCode()).isEqualTo(ApiResponseCode.CANNOT_MANAGE_HIGHER_POSITION));
        }

        @Test
        @DisplayName("부회장이 이미 존재하면 VICE_PRESIDENT_ALREADY_EXISTS 예외가 발생한다")
        void changeMemberPositionWhenVicePresidentExistsThrowsCustomException() {
            // Given
            MemberPositionChangeRequest request = new MemberPositionChangeRequest(ClubPosition.VICE_PRESIDENT);
            ClubMember requester = createMember(CLUB_ID, REQUESTER_ID, ClubPosition.PRESIDENT);
            ClubMember target = createMember(CLUB_ID, TARGET_ID, ClubPosition.MEMBER);

            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, REQUESTER_ID)).thenReturn(requester);
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, TARGET_ID)).thenReturn(target);
            when(clubMemberRepository.countByClubIdAndPosition(CLUB_ID, ClubPosition.VICE_PRESIDENT)).thenReturn(1L);

            // When & Then
            assertThatThrownBy(() -> clubMemberManagementService.changeMemberPosition(
                CLUB_ID,
                TARGET_ID,
                REQUESTER_ID,
                request
            )).isInstanceOfSatisfying(CustomException.class, ex -> assertThat(
                ex.getErrorCode()).isEqualTo(ApiResponseCode.VICE_PRESIDENT_ALREADY_EXISTS));
        }

        @Test
        @DisplayName("운영진 수가 제한에 도달하면 MANAGER_LIMIT_EXCEEDED 예외가 발생한다")
        void changeMemberPositionWhenManagerLimitReachedThrowsCustomException() {
            // Given
            MemberPositionChangeRequest request = new MemberPositionChangeRequest(ClubPosition.MANAGER);
            ClubMember requester = createMember(CLUB_ID, REQUESTER_ID, ClubPosition.PRESIDENT);
            ClubMember target = createMember(CLUB_ID, TARGET_ID, ClubPosition.MEMBER);

            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, REQUESTER_ID)).thenReturn(requester);
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, TARGET_ID)).thenReturn(target);
            when(clubMemberRepository.countByClubIdAndPosition(CLUB_ID, ClubPosition.MANAGER))
                .thenReturn((long)ClubMemberManagementService.MAX_MANAGER_COUNT);

            // When & Then
            assertThatThrownBy(() -> clubMemberManagementService.changeMemberPosition(
                CLUB_ID,
                TARGET_ID,
                REQUESTER_ID,
                request
            )).isInstanceOfSatisfying(CustomException.class, ex -> assertThat(
                ex.getErrorCode()).isEqualTo(ApiResponseCode.MANAGER_LIMIT_EXCEEDED));
        }

        @Test
        @DisplayName("권한 검증은 항상 leader access 검증을 호출한다")
        void changeMemberPositionCallsLeaderAccessValidation() {
            // Given
            MemberPositionChangeRequest request = new MemberPositionChangeRequest(ClubPosition.MEMBER);
            ClubMember requester = createMember(CLUB_ID, REQUESTER_ID, ClubPosition.PRESIDENT);
            ClubMember target = createMember(CLUB_ID, TARGET_ID, ClubPosition.MEMBER);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, REQUESTER_ID)).thenReturn(requester);
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, TARGET_ID)).thenReturn(target);

            // When
            clubMemberManagementService.changeMemberPosition(CLUB_ID, TARGET_ID, REQUESTER_ID, request);

            // Then
            verify(clubPermissionValidator).validateLeaderAccess(CLUB_ID, REQUESTER_ID);
        }

        @Test
        @DisplayName("요청자가 변경하려는 직책을 관리할 수 없으면 FORBIDDEN_MEMBER_POSITION_CHANGE 예외가 발생한다")
        void changeMemberPositionWithUnmanageableNewPositionThrowsCustomException() {
            // Given
            MemberPositionChangeRequest request = new MemberPositionChangeRequest(ClubPosition.PRESIDENT);
            ClubMember requester = createMember(CLUB_ID, REQUESTER_ID, ClubPosition.MANAGER);
            ClubMember target = createMember(CLUB_ID, TARGET_ID, ClubPosition.MEMBER);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, REQUESTER_ID)).thenReturn(requester);
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, TARGET_ID)).thenReturn(target);

            // When & Then
            assertThatThrownBy(() -> clubMemberManagementService.changeMemberPosition(
                CLUB_ID,
                TARGET_ID,
                REQUESTER_ID,
                request
            )).isInstanceOfSatisfying(CustomException.class, ex -> assertThat(
                ex.getErrorCode()).isEqualTo(ApiResponseCode.FORBIDDEN_MEMBER_POSITION_CHANGE));
        }

        @Test
        @DisplayName("대상이 이미 부회장이면 부회장 제한 검사를 건너뛴다")
        void changeMemberPositionWhenTargetAlreadyVicePresidentSkipsVicePresidentLimitValidation() {
            // Given
            MemberPositionChangeRequest request = new MemberPositionChangeRequest(ClubPosition.VICE_PRESIDENT);
            ClubMember requester = createMember(CLUB_ID, REQUESTER_ID, ClubPosition.PRESIDENT);
            ClubMember target = createMember(CLUB_ID, TARGET_ID, ClubPosition.VICE_PRESIDENT);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, REQUESTER_ID)).thenReturn(requester);
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, TARGET_ID)).thenReturn(target);

            // When
            ClubMember changed = clubMemberManagementService.changeMemberPosition(
                CLUB_ID,
                TARGET_ID,
                REQUESTER_ID,
                request
            );

            // Then
            assertThat(changed.getClubPosition()).isEqualTo(ClubPosition.VICE_PRESIDENT);
            verify(clubMemberRepository, never())
                .countByClubIdAndPosition(CLUB_ID, ClubPosition.VICE_PRESIDENT);
        }

        @Test
        @DisplayName("대상이 이미 운영진이면 운영진 제한 검사를 건너뛴다")
        void changeMemberPositionWhenTargetAlreadyManagerSkipsManagerLimitValidation() {
            // Given
            MemberPositionChangeRequest request = new MemberPositionChangeRequest(ClubPosition.MANAGER);
            ClubMember requester = createMember(CLUB_ID, REQUESTER_ID, ClubPosition.PRESIDENT);
            ClubMember target = createMember(CLUB_ID, TARGET_ID, ClubPosition.MANAGER);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, REQUESTER_ID)).thenReturn(requester);
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, TARGET_ID)).thenReturn(target);

            // When
            ClubMember changed = clubMemberManagementService.changeMemberPosition(
                CLUB_ID,
                TARGET_ID,
                REQUESTER_ID,
                request
            );

            // Then
            assertThat(changed.getClubPosition()).isEqualTo(ClubPosition.MANAGER);
            verify(clubMemberRepository, never())
                .countByClubIdAndPosition(CLUB_ID, ClubPosition.MANAGER);
        }
    }

    @Nested
    @DisplayName("addPreMember 테스트")
    class AddPreMemberTests {

        @Test
        @DisplayName("중복 사전등록이면 ALREADY_CLUB_PRE_MEMBER 예외가 발생한다")
        void addPreMemberWithDuplicateThrowsCustomException() {
            // Given
            ClubPreMemberAddRequest request = new ClubPreMemberAddRequest(STUDENT_NUMBER, "홍길동");
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubPreMemberRepository.existsByClubIdAndStudentNumberAndName(CLUB_ID, STUDENT_NUMBER, "홍길동"))
                .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> clubMemberManagementService.addPreMember(CLUB_ID, REQUESTER_ID, request))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(ex.getErrorCode())
                    .isEqualTo(ApiResponseCode.ALREADY_CLUB_PRE_MEMBER));
        }

        @Test
        @DisplayName("정상 사전등록이면 저장 후 응답을 반환한다")
        void addPreMemberWithValidRequestReturnsSavedResponse() {
            // Given
            Club club = createClub(CLUB_ID);
            ClubPreMemberAddRequest request = new ClubPreMemberAddRequest(STUDENT_NUMBER, "홍길동");
            ClubPreMember saved = ClubPreMember.builder()
                .id(1)
                .club(club)
                .studentNumber(STUDENT_NUMBER)
                .name("홍길동")
                .build();
            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(clubPreMemberRepository.existsByClubIdAndStudentNumberAndName(CLUB_ID, STUDENT_NUMBER, "홍길동"))
                .thenReturn(false);
            when(clubPreMemberRepository.save(any(ClubPreMember.class))).thenReturn(saved);

            // When
            var response = clubMemberManagementService.addPreMember(CLUB_ID, REQUESTER_ID, request);

            // Then
            assertThat(response.clubId()).isEqualTo(CLUB_ID);
            assertThat(response.studentNumber()).isEqualTo(STUDENT_NUMBER);
            assertThat(response.name()).isEqualTo("홍길동");
        }
    }

    @Nested
    @DisplayName("transferPresident 테스트")
    class TransferPresidentTests {

        @Test
        @DisplayName("자기 자신에게 회장 이양 시 ILLEGAL_ARGUMENT 예외가 발생한다")
        void transferPresidentToSelfThrowsCustomException() {
            // Given
            PresidentTransferRequest request = new PresidentTransferRequest(REQUESTER_ID);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, REQUESTER_ID))
                .thenReturn(createMember(CLUB_ID, REQUESTER_ID, ClubPosition.PRESIDENT));

            // When & Then
            assertThatThrownBy(() -> clubMemberManagementService.transferPresident(CLUB_ID, REQUESTER_ID, request))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(ex.getErrorCode())
                    .isEqualTo(ApiResponseCode.ILLEGAL_ARGUMENT));
        }

        @Test
        @DisplayName("정상 이양 시 기존 회장은 MEMBER, 신규 회장은 PRESIDENT가 된다")
        void transferPresidentWithValidRequestChangesPositions() {
            // Given
            PresidentTransferRequest request = new PresidentTransferRequest(NEW_LEADER_ID);
            ClubMember currentPresident = createMember(CLUB_ID, REQUESTER_ID, ClubPosition.PRESIDENT);
            ClubMember newPresident = createMember(CLUB_ID, NEW_LEADER_ID, ClubPosition.MANAGER);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, REQUESTER_ID)).thenReturn(currentPresident);
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, NEW_LEADER_ID)).thenReturn(newPresident);

            // When
            List<ClubMember> changedMembers = clubMemberManagementService.transferPresident(
                CLUB_ID,
                REQUESTER_ID,
                request
            );

            // Then
            assertThat(changedMembers).hasSize(2);
            assertThat(currentPresident.getClubPosition()).isEqualTo(ClubPosition.MEMBER);
            assertThat(newPresident.getClubPosition()).isEqualTo(ClubPosition.PRESIDENT);
            verify(clubPermissionValidator).validatePresidentAccess(CLUB_ID, REQUESTER_ID);
        }
    }

    @Nested
    @DisplayName("changeVicePresident 테스트")
    class ChangeVicePresidentTests {

        @Test
        @DisplayName("신규 부회장 아이디가 null이고 기존 부회장이 없으면 빈 리스트를 반환한다")
        void changeVicePresidentWithNullAndNoCurrentVicePresidentReturnsEmptyList() {
            // Given
            VicePresidentChangeRequest request = new VicePresidentChangeRequest(null);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.findAllByClubIdAndPosition(CLUB_ID, ClubPosition.VICE_PRESIDENT))
                .thenReturn(List.of());

            // When
            List<ClubMember> changed = clubMemberManagementService.changeVicePresident(CLUB_ID, REQUESTER_ID, request);

            // Then
            assertThat(changed).isEmpty();
        }

        @Test
        @DisplayName("신규 부회장 아이디가 null이고 기존 부회장이 있으면 MEMBER로 강등한다")
        void changeVicePresidentWithNullAndCurrentVicePresidentDemotesCurrentVicePresident() {
            // Given
            VicePresidentChangeRequest request = new VicePresidentChangeRequest(null);
            ClubMember currentVicePresident = createMember(CLUB_ID, TARGET_ID, ClubPosition.VICE_PRESIDENT);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.findAllByClubIdAndPosition(CLUB_ID, ClubPosition.VICE_PRESIDENT))
                .thenReturn(List.of(currentVicePresident));

            // When
            List<ClubMember> changed = clubMemberManagementService.changeVicePresident(CLUB_ID, REQUESTER_ID, request);

            // Then
            assertThat(changed).hasSize(1);
            assertThat(changed.get(0).getClubPosition()).isEqualTo(ClubPosition.MEMBER);
        }

        @Test
        @DisplayName("본인을 부회장으로 변경하려 하면 CANNOT_CHANGE_OWN_POSITION 예외가 발생한다")
        void changeVicePresidentWithSelfTargetThrowsCustomException() {
            // Given
            VicePresidentChangeRequest request = new VicePresidentChangeRequest(REQUESTER_ID);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.findAllByClubIdAndPosition(CLUB_ID, ClubPosition.VICE_PRESIDENT))
                .thenReturn(List.of());

            // When & Then
            assertThatThrownBy(() -> clubMemberManagementService.changeVicePresident(CLUB_ID, REQUESTER_ID, request))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(ex.getErrorCode())
                    .isEqualTo(ApiResponseCode.CANNOT_CHANGE_OWN_POSITION));
        }

        @Test
        @DisplayName("기존 부회장과 다른 사용자로 변경하면 기존은 MEMBER, 신규는 VICE_PRESIDENT가 된다")
        void changeVicePresidentWithDifferentTargetDemotesCurrentAndPromotesNewOne() {
            // Given
            VicePresidentChangeRequest request = new VicePresidentChangeRequest(NEW_LEADER_ID);
            ClubMember currentVicePresident = createMember(CLUB_ID, TARGET_ID, ClubPosition.VICE_PRESIDENT);
            ClubMember newVicePresident = createMember(CLUB_ID, NEW_LEADER_ID, ClubPosition.MEMBER);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.findAllByClubIdAndPosition(CLUB_ID, ClubPosition.VICE_PRESIDENT))
                .thenReturn(List.of(currentVicePresident));
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, NEW_LEADER_ID)).thenReturn(newVicePresident);

            // When
            List<ClubMember> changed = clubMemberManagementService.changeVicePresident(CLUB_ID, REQUESTER_ID, request);

            // Then
            assertThat(changed).hasSize(2);
            assertThat(currentVicePresident.getClubPosition()).isEqualTo(ClubPosition.MEMBER);
            assertThat(newVicePresident.getClubPosition()).isEqualTo(ClubPosition.VICE_PRESIDENT);
        }

        @Test
        @DisplayName("기존 부회장을 그대로 지정하면 기존 부회장만 반환한다")
        void changeVicePresidentWithSameTargetReturnsOnlyTarget() {
            // Given
            VicePresidentChangeRequest request = new VicePresidentChangeRequest(TARGET_ID);
            ClubMember currentVicePresident = createMember(CLUB_ID, TARGET_ID, ClubPosition.VICE_PRESIDENT);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.findAllByClubIdAndPosition(CLUB_ID, ClubPosition.VICE_PRESIDENT))
                .thenReturn(List.of(currentVicePresident));
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, TARGET_ID)).thenReturn(currentVicePresident);

            // When
            List<ClubMember> changed = clubMemberManagementService.changeVicePresident(CLUB_ID, REQUESTER_ID, request);

            // Then
            assertThat(changed).hasSize(1);
            assertThat(changed.get(0).getId().getUserId()).isEqualTo(TARGET_ID);
            assertThat(changed.get(0).getClubPosition()).isEqualTo(ClubPosition.VICE_PRESIDENT);
        }
    }

    @Nested
    @DisplayName("removeMember 테스트")
    class RemoveMemberTests {

        @Test
        @DisplayName("자기 자신 제거 시 CANNOT_REMOVE_SELF 예외가 발생한다")
        void removeMemberWithSelfTargetThrowsCustomException() {
            // Given
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));

            // When & Then
            assertThatThrownBy(() -> clubMemberManagementService.removeMember(CLUB_ID, REQUESTER_ID, REQUESTER_ID))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(ex.getErrorCode())
                    .isEqualTo(ApiResponseCode.CANNOT_REMOVE_SELF));
        }

        @Test
        @DisplayName("대상이 회장이면 CANNOT_DELETE_CLUB_PRESIDENT 예외가 발생한다")
        void removeMemberWithPresidentTargetThrowsCustomException() {
            // Given
            ClubMember requester = createMember(CLUB_ID, REQUESTER_ID, ClubPosition.PRESIDENT);
            ClubMember target = createMember(CLUB_ID, TARGET_ID, ClubPosition.PRESIDENT);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, REQUESTER_ID)).thenReturn(requester);
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, TARGET_ID)).thenReturn(target);

            // When & Then
            assertThatThrownBy(() -> clubMemberManagementService.removeMember(CLUB_ID, TARGET_ID, REQUESTER_ID))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(ex.getErrorCode())
                    .isEqualTo(ApiResponseCode.CANNOT_DELETE_CLUB_PRESIDENT));
        }

        @Test
        @DisplayName("요청자가 대상을 관리할 수 없으면 CANNOT_MANAGE_HIGHER_POSITION 예외가 발생한다")
        void removeMemberWithUnmanageableTargetThrowsCustomException() {
            // Given
            ClubMember requester = createMember(CLUB_ID, REQUESTER_ID, ClubPosition.MANAGER);
            ClubMember target = createMember(CLUB_ID, TARGET_ID, ClubPosition.VICE_PRESIDENT);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, REQUESTER_ID)).thenReturn(requester);
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, TARGET_ID)).thenReturn(target);

            // When & Then
            assertThatThrownBy(() -> clubMemberManagementService.removeMember(CLUB_ID, TARGET_ID, REQUESTER_ID))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(ex.getErrorCode())
                    .isEqualTo(ApiResponseCode.CANNOT_MANAGE_HIGHER_POSITION));
        }

        @Test
        @DisplayName("대상이 MEMBER가 아니면 CANNOT_REMOVE_NON_MEMBER 예외가 발생한다")
        void removeMemberWithNonMemberTargetThrowsCustomException() {
            // Given
            ClubMember requester = createMember(CLUB_ID, REQUESTER_ID, ClubPosition.PRESIDENT);
            ClubMember target = createMember(CLUB_ID, TARGET_ID, ClubPosition.MANAGER);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, REQUESTER_ID)).thenReturn(requester);
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, TARGET_ID)).thenReturn(target);

            // When & Then
            assertThatThrownBy(() -> clubMemberManagementService.removeMember(CLUB_ID, TARGET_ID, REQUESTER_ID))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(ex.getErrorCode())
                    .isEqualTo(ApiResponseCode.CANNOT_REMOVE_NON_MEMBER));
        }

        @Test
        @DisplayName("정상 제거 시 대상 MEMBER를 삭제한다")
        void removeMemberWithValidMemberDeletesTarget() {
            // Given
            ClubMember requester = createMember(CLUB_ID, REQUESTER_ID, ClubPosition.PRESIDENT);
            ClubMember target = createMember(CLUB_ID, TARGET_ID, ClubPosition.MEMBER);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, REQUESTER_ID)).thenReturn(requester);
            when(clubMemberRepository.getByClubIdAndUserId(CLUB_ID, TARGET_ID)).thenReturn(target);

            // When
            clubMemberManagementService.removeMember(CLUB_ID, TARGET_ID, REQUESTER_ID);

            // Then
            verify(clubMemberRepository).delete(target);
        }
    }

    private Club createClub(Integer clubId) {
        return Club.builder()
            .id(clubId)
            .name("BCSD")
            .description("desc")
            .introduce("intro")
            .imageUrl("https://img")
            .location("room")
            .clubCategory(ClubCategory.ACADEMIC)
            .university(University.builder().id(UNIVERSITY_ID).koreanName("경북대").campus(Campus.MAIN).build())
            .build();
    }

    private ClubMember createMember(Integer clubId, Integer userId, ClubPosition position) {
        Club club = createClub(clubId);
        User user = User.builder()
            .id(userId)
            .name("user-" + userId)
            .email("u" + userId + "@konect.gg")
            .studentNumber(STUDENT_NUMBER)
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

}
