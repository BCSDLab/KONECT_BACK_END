package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.dto.ClubRecruitmentResponse;
import gg.agit.konect.domain.club.dto.ClubRecruitmentUpsertRequest;
import gg.agit.konect.domain.club.enums.RecruitmentStatus;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubRecruitment;
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
class ClubRecruitmentServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ClubRecruitmentService clubRecruitmentService;

    @Autowired
    private ClubRecruitmentRepository clubRecruitmentRepository;

    private University university;
    private User normalUser;
    private User president;
    private User manager;
    private Club club;

    @BeforeEach
    void setUp() {
        university = persist(UniversityFixture.create());
        normalUser = persist(UserFixture.createUser(university, "일반유저", "2021136001"));
        president = persist(UserFixture.createUser(university, "회장", "2020000001"));
        manager = persist(UserFixture.createUser(university, "매니저", "2020000002"));
        club = persist(ClubFixture.createWithRecruitment(university, "BCSD Lab"));
        persist(ClubMemberFixture.createPresident(club, president));
        persist(ClubMemberFixture.createManager(club, manager));
    }

    @Nested
    @DisplayName("모집 공고 조회")
    class GetRecruitment {

        @Test
        @DisplayName("모집 공고를 조회한다")
        void getRecruitmentSuccess() {
            // given
            ClubRecruitment recruitment = persist(ClubRecruitmentFixture.createAlwaysRecruiting(club));
            clearPersistenceContext();

            // when
            ClubRecruitmentResponse response = clubRecruitmentService.getRecruitment(
                club.getId(),
                normalUser.getId()
            );

            // then
            assertThat(response.content()).isEqualTo("상시 모집 공고 내용입니다.");
            assertThat(response.status()).isEqualTo(RecruitmentStatus.ONGOING);
            assertThat(response.startAt()).isNull();
            assertThat(response.endAt()).isNull();
            assertThat(response.isApplied()).isFalse();
        }

        @Test
        @DisplayName("기간 모집 공고를 조회한다")
        void getRecruitmentWithPeriod() {
            // given
            LocalDateTime startAt = LocalDateTime.now().minusDays(1);
            LocalDateTime endAt = LocalDateTime.now().plusDays(30);
            ClubRecruitment recruitment = persist(
                ClubRecruitmentFixture.createWithPeriod(club, startAt, endAt)
            );
            clearPersistenceContext();

            // when
            ClubRecruitmentResponse response = clubRecruitmentService.getRecruitment(
                club.getId(),
                normalUser.getId()
            );

            // then
            assertThat(response.status()).isEqualTo(RecruitmentStatus.ONGOING);
            assertThat(response.startAt()).isNotNull();
            assertThat(response.endAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("모집 공고 생성/수정")
    class UpsertRecruitment {

        @Test
        @DisplayName("회장이 상시 모집 공고를 생성한다")
        void createAlwaysRecruitmentByPresident() {
            // given
            ClubRecruitmentUpsertRequest request = new ClubRecruitmentUpsertRequest(
                null,
                null,
                true,
                "새로운 상시 모집 공고입니다.",
                List.of()
            );

            // when
            clubRecruitmentService.upsertRecruitment(club.getId(), president.getId(), request);
            clearPersistenceContext();

            // then
            ClubRecruitment saved = clubRecruitmentRepository.getByClubId(club.getId());
            assertThat(saved.getContent()).isEqualTo("새로운 상시 모집 공고입니다.");
            assertThat(saved.getIsAlwaysRecruiting()).isTrue();
        }

        private ClubRecruitmentUpsertRequest.InnerClubRecruitmentImageRequest imageRequest(String url) {
            return new ClubRecruitmentUpsertRequest.InnerClubRecruitmentImageRequest(url);
        }

        @Test
        @DisplayName("매니저가 기간 모집 공고를 생성한다")
        void createPeriodRecruitmentByManager() {
            // given
            LocalDateTime startAt = LocalDateTime.now().plusDays(1);
            LocalDateTime endAt = LocalDateTime.now().plusDays(30);

            ClubRecruitmentUpsertRequest request = new ClubRecruitmentUpsertRequest(
                startAt,
                endAt,
                false,
                "기간 모집 공고입니다.",
                List.of(
                    imageRequest("https://example.com/image1.png"),
                    imageRequest("https://example.com/image2.png")
                )
            );

            // when
            clubRecruitmentService.upsertRecruitment(club.getId(), manager.getId(), request);
            clearPersistenceContext();

            // then
            ClubRecruitment saved = clubRecruitmentRepository.getByClubId(club.getId());
            assertThat(saved.getContent()).isEqualTo("기간 모집 공고입니다.");
            assertThat(saved.getIsAlwaysRecruiting()).isFalse();
            assertThat(saved.getStartAt()).isNotNull();
            assertThat(saved.getEndAt()).isNotNull();
            assertThat(saved.getImages()).hasSize(2);
        }

        @Test
        @DisplayName("기존 모집 공고를 수정한다")
        void updateExistingRecruitment() {
            // given
            persist(ClubRecruitmentFixture.createAlwaysRecruiting(club));
            clearPersistenceContext();

            LocalDateTime startAt = LocalDateTime.now().plusDays(1);
            LocalDateTime endAt = LocalDateTime.now().plusDays(30);

            ClubRecruitmentUpsertRequest request = new ClubRecruitmentUpsertRequest(
                startAt,
                endAt,
                false,
                "수정된 모집 공고입니다.",
                List.of()
            );

            // when
            clubRecruitmentService.upsertRecruitment(club.getId(), president.getId(), request);
            clearPersistenceContext();

            // then
            ClubRecruitment updated = clubRecruitmentRepository.getByClubId(club.getId());
            assertThat(updated.getContent()).isEqualTo("수정된 모집 공고입니다.");
            assertThat(updated.getIsAlwaysRecruiting()).isFalse();
        }

        @Test
        @DisplayName("일반 유저가 모집 공고 생성 시 예외가 발생한다")
        void createRecruitmentByNormalUserFails() {
            // given
            ClubRecruitmentUpsertRequest request = new ClubRecruitmentUpsertRequest(
                null,
                null,
                true,
                "모집 공고 내용",
                List.of()
            );

            // when & then
            assertThatThrownBy(() ->
                clubRecruitmentService.upsertRecruitment(club.getId(), normalUser.getId(), request)
            ).isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("상시 모집에 날짜를 입력하면 예외가 발생한다")
        void alwaysRecruitingWithDatesFails() {
            // given
            ClubRecruitmentUpsertRequest request = new ClubRecruitmentUpsertRequest(
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                true,
                "상시 모집인데 날짜가 있음",
                List.of()
            );

            // when & then
            assertThatThrownBy(() ->
                clubRecruitmentService.upsertRecruitment(club.getId(), president.getId(), request)
            ).isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("기간 모집에 날짜가 없으면 예외가 발생한다")
        void periodRecruitingWithoutDatesFails() {
            // given
            ClubRecruitmentUpsertRequest request = new ClubRecruitmentUpsertRequest(
                null,
                null,
                false,
                "기간 모집인데 날짜가 없음",
                List.of()
            );

            // when & then
            assertThatThrownBy(() ->
                clubRecruitmentService.upsertRecruitment(club.getId(), president.getId(), request)
            ).isInstanceOf(CustomException.class);
        }
    }
}
