package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.dto.ClubApplicationCondition;
import gg.agit.konect.domain.club.dto.ClubApplicationsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyRequest;
import gg.agit.konect.domain.club.enums.ClubApplicationSortBy;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubApply;
import gg.agit.konect.domain.club.model.ClubApplyQuestion;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.repository.ClubApplyQuestionRepository;
import gg.agit.konect.domain.club.repository.ClubApplyRepository;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRecruitmentRepository;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.ClubRecruitmentFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

@Transactional
class ClubApplicationServiceIntegrationTest extends IntegrationTestSupport {

    private static final int DEFAULT_PAGE_SIZE = 10;

    @Autowired
    private ClubApplicationService clubApplicationService;

    @Autowired
    private ClubApplyRepository clubApplyRepository;

    @Autowired
    private ClubApplyQuestionRepository clubApplyQuestionRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubRecruitmentRepository clubRecruitmentRepository;

    private University university;
    private User applicant;
    private User president;
    private Club club;
    private ClubRecruitment recruitment;

    @BeforeEach
    void setUp() {
        university = persist(UniversityFixture.create());
        applicant = persist(UserFixture.createUser(university, "지원자", "2021136001"));
        president = persist(UserFixture.createUser(university, "회장", "2020000001"));
        club = persist(ClubFixture.createWithRecruitment(university, "BCSD Lab"));
        persist(ClubMemberFixture.createPresident(club, president));
        recruitment = persist(ClubRecruitmentFixture.createAlwaysRecruiting(club));
    }

    @Nested
    @DisplayName("동아리 가입 신청")
    class ApplyClub {

        @Test
        @DisplayName("동아리에 가입 신청한다")
        void applyClubSuccess() {
            // given
            ClubApplyQuestion question = persist(ClubApplyQuestion.of(club, "지원 동기", false, 1));
            clearPersistenceContext();

            ClubApplyRequest request = new ClubApplyRequest(
                List.of(new ClubApplyRequest.InnerClubQuestionAnswer(question.getId(), "동아리 활동을 하고 싶습니다.")),
                null
            );

            // when
            clubApplicationService.applyClub(club.getId(), applicant.getId(), request);

            // then
            boolean hasApplied = clubApplyRepository.existsPendingByClubIdAndUserId(club.getId(), applicant.getId());
            assertThat(hasApplied).isTrue();
        }

        @Test
        @DisplayName("이미 가입 신청한 동아리에 다시 신청하면 예외가 발생한다")
        void applyClubDuplicateFails() {
            // given
            ClubApply existingApply = ClubApply.of(club, applicant, null);
            persist(existingApply);
            clearPersistenceContext();

            ClubApplyRequest request = new ClubApplyRequest(List.of(), null);

            // when & then
            assertThatThrownBy(() -> clubApplicationService.applyClub(club.getId(), applicant.getId(), request))
                .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("이미 동아리 멤버인 경우 신청하면 예외가 발생한다")
        void applyClubWhenAlreadyMemberFails() {
            // given
            persist(ClubMemberFixture.createMember(club, applicant));
            clearPersistenceContext();

            ClubApplyRequest request = new ClubApplyRequest(List.of(), null);

            // when & then
            assertThatThrownBy(() -> clubApplicationService.applyClub(club.getId(), applicant.getId(), request))
                .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("지원자 목록 조회")
    class GetClubApplications {

        @Test
        @DisplayName("동아리 지원자 목록을 조회한다")
        void getClubApplicationsSuccess() {
            // given
            User applicant1 = persist(UserFixture.createUser(university, "지원자1", "2021000001"));
            User applicant2 = persist(UserFixture.createUser(university, "지원자2", "2021000002"));
            persist(ClubApply.of(club, applicant1, null));
            persist(ClubApply.of(club, applicant2, null));
            clearPersistenceContext();

            ClubApplicationCondition condition = new ClubApplicationCondition(
                1, DEFAULT_PAGE_SIZE, ClubApplicationSortBy.APPLIED_AT, Sort.Direction.ASC
            );

            // when
            ClubApplicationsResponse response = clubApplicationService.getClubApplications(
                club.getId(),
                president.getId(),
                condition
            );

            // then
            assertThat(response.applications()).hasSize(2);
        }

        @Test
        @DisplayName("승인된 멤버는 지원자 목록에 표시되지 않는다")
        void approvedMembersNotInApplicationList() {
            // given
            User applicant1 = persist(UserFixture.createUser(university, "지원자1", "2021000001"));
            User applicant2 = persist(UserFixture.createUser(university, "지원자2", "2021000002"));

            ClubApply apply1 = persist(ClubApply.of(club, applicant1, null));
            persist(ClubApply.of(club, applicant2, null));

            // applicant1을 승인
            apply1.approve();
            persist(ClubMemberFixture.createMember(club, applicant1));
            clearPersistenceContext();

            ClubApplicationCondition condition = new ClubApplicationCondition(
                1, DEFAULT_PAGE_SIZE, ClubApplicationSortBy.APPLIED_AT, Sort.Direction.ASC
            );

            // when
            ClubApplicationsResponse response = clubApplicationService.getClubApplications(
                club.getId(),
                president.getId(),
                condition
            );

            // then
            assertThat(response.applications()).hasSize(1);
            assertThat(response.applications().get(0).name()).isEqualTo("지원자2");
        }
    }

    @Nested
    @DisplayName("지원 승인")
    class ApproveClubApplication {

        @Test
        @DisplayName("지원자를 승인하면 동아리 멤버가 된다")
        void approveClubApplicationSuccess() {
            // given
            ClubApply apply = clubApplyRepository.save(ClubApply.of(club, applicant, null));
            entityManager.flush();

            // when
            clubApplicationService.approveClubApplication(club.getId(), apply.getId(), president.getId());
            entityManager.flush();
            entityManager.clear();

            // then
            boolean isMember = clubMemberRepository.existsByClubIdAndUserId(club.getId(), applicant.getId());
            assertThat(isMember).isTrue();
        }

        @Test
        @DisplayName("이미 멤버인 사용자를 승인하면 예외가 발생한다")
        void approveAlreadyMemberFails() {
            // given
            ClubApply apply = clubApplyRepository.save(ClubApply.of(club, applicant, null));
            persist(ClubMemberFixture.createMember(club, applicant));
            entityManager.flush();

            // when & then
            assertThatThrownBy(() ->
                clubApplicationService.approveClubApplication(club.getId(), apply.getId(), president.getId())
            ).isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("지원 거절")
    class RejectClubApplication {

        @Test
        @DisplayName("지원자를 거절하면 멤버가 되지 않는다")
        void rejectClubApplicationSuccess() {
            // given
            ClubApply apply = clubApplyRepository.save(ClubApply.of(club, applicant, null));
            entityManager.flush();

            // when
            clubApplicationService.rejectClubApplication(club.getId(), apply.getId(), president.getId());
            entityManager.flush();
            entityManager.clear();

            // then
            boolean isMember = clubMemberRepository.existsByClubIdAndUserId(club.getId(), applicant.getId());
            assertThat(isMember).isFalse();
        }
    }

    @Nested
    @DisplayName("승인된 멤버 지원서 목록 조회")
    class GetApprovedMemberApplications {

        @Test
        @DisplayName("승인된 멤버들의 지원서 목록을 조회한다")
        void getApprovedMemberApplicationsSuccess() {
            // given
            User applicant1 = persist(UserFixture.createUser(university, "승인멤버1", "2021000001"));
            User applicant2 = persist(UserFixture.createUser(university, "승인멤버2", "2021000002"));
            User applicant3 = persist(UserFixture.createUser(university, "대기중지원자", "2021000003"));

            ClubApply apply1 = persist(ClubApply.of(club, applicant1, null));
            ClubApply apply2 = persist(ClubApply.of(club, applicant2, null));
            persist(ClubApply.of(club, applicant3, null)); // 아직 대기중인 지원자

            // applicant1, applicant2 승인
            apply1.approve();
            apply2.approve();
            persist(ClubMemberFixture.createMember(club, applicant1));
            persist(ClubMemberFixture.createMember(club, applicant2));
            clearPersistenceContext();

            ClubApplicationCondition condition = new ClubApplicationCondition(
                1, DEFAULT_PAGE_SIZE, ClubApplicationSortBy.APPLIED_AT, Sort.Direction.ASC
            );

            // when
            ClubApplicationsResponse response = clubApplicationService.getApprovedMemberApplications(
                club.getId(),
                president.getId(),
                condition
            );

            // then
            assertThat(response.applications()).hasSize(2);
        }
    }
}
