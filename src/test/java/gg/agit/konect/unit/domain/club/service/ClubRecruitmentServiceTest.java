package gg.agit.konect.unit.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.club.dto.ClubRecruitmentResponse;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.repository.ClubApplyRepository;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRecruitmentRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.club.service.ClubPermissionValidator;
import gg.agit.konect.domain.club.service.ClubRecruitmentService;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class ClubRecruitmentServiceTest extends ServiceTestSupport {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubRecruitmentRepository clubRecruitmentRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private ClubApplyRepository clubApplyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClubPermissionValidator clubPermissionValidator;

    @InjectMocks
    private ClubRecruitmentService clubRecruitmentService;

    @Test
    @DisplayName("getRecruitment는 현재 회원이면 isApplied=true를 반환한다")
    void getRecruitmentMarksAppliedForMember() {
        // given
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), 1, "BCSD");
        User user = UserFixture.createUserWithId(
            UniversityFixture.createWithId(1),
            10,
            "회원",
            "20240001",
            UserRole.USER
        );
        ClubRecruitment recruitment = ClubRecruitment.of(
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(1),
            false,
            "모집 공고",
            club
        );

        given(clubRepository.getById(1)).willReturn(club);
        given(userRepository.getById(10)).willReturn(user);
        given(clubRecruitmentRepository.getByClubId(1)).willReturn(recruitment);
        given(clubMemberRepository.existsByClubIdAndUserId(1, 10)).willReturn(true);

        // when
        ClubRecruitmentResponse response = clubRecruitmentService.getRecruitment(1, 10);

        // then
        assertThat(response.isApplied()).isTrue();
    }

    @Test
    @DisplayName("getRecruitment는 회원이 아니어도 pending 지원이 있으면 isApplied=true를 반환한다")
    void getRecruitmentMarksAppliedForPendingApplicant() {
        // given
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), 1, "BCSD");
        User user = UserFixture.createUserWithId(
            UniversityFixture.createWithId(1),
            10,
            "지원자",
            "20240001",
            UserRole.USER
        );
        ClubRecruitment recruitment = ClubRecruitment.of(
            null,
            null,
            true,
            "상시 모집",
            club
        );

        given(clubRepository.getById(1)).willReturn(club);
        given(userRepository.getById(10)).willReturn(user);
        given(clubRecruitmentRepository.getByClubId(1)).willReturn(recruitment);
        given(clubMemberRepository.existsByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyRepository.existsPendingByClubIdAndUserId(1, 10)).willReturn(true);

        // when
        ClubRecruitmentResponse response = clubRecruitmentService.getRecruitment(1, 10);

        // then
        assertThat(response.isApplied()).isTrue();
    }
}
