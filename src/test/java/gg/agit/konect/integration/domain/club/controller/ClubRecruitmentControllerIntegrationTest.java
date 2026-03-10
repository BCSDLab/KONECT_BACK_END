package gg.agit.konect.integration.domain.club.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import gg.agit.konect.domain.club.dto.ClubRecruitmentUpsertRequest;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.repository.ClubRecruitmentRepository;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.ClubRecruitmentFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class ClubRecruitmentControllerIntegrationTest extends IntegrationTestSupport {

    private static final int RECRUITMENT_PERIOD_DAYS = 30;

    @Autowired
    private ClubRecruitmentRepository clubRecruitmentRepository;

    private University university;
    private User normalUser;
    private User president;
    private User manager;
    private Club club;

    @BeforeEach
    void setUp() throws Exception {
        university = persist(UniversityFixture.create());
        normalUser = persist(UserFixture.createUser(university, "일반유저", "2021136001"));
        president = persist(UserFixture.createUser(university, "회장", "2020000001"));
        manager = persist(UserFixture.createUser(university, "매니저", "2020000002"));
        club = persist(ClubFixture.createWithRecruitment(university, "BCSD Lab"));
        persist(ClubMemberFixture.createPresident(club, president));
        persist(ClubMemberFixture.createManager(club, manager));
    }

    @Nested
    @DisplayName("GET /clubs/{clubId}/recruitments - 모집 공고 조회")
    class GetRecruitment {

        @Test
        @DisplayName("상시 모집 공고를 조회한다")
        void getRecruitmentSuccess() throws Exception {
            // given
            persist(ClubRecruitmentFixture.createAlwaysRecruiting(club));
            clearPersistenceContext();

            mockLoginUser(normalUser.getId());

            // when & then
            performGet("/clubs/" + club.getId() + "/recruitments")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("상시 모집 공고 내용입니다."))
                .andExpect(jsonPath("$.status").value("ONGOING"))
                .andExpect(jsonPath("$.startAt").isEmpty())
                .andExpect(jsonPath("$.endAt").isEmpty())
                .andExpect(jsonPath("$.isApplied").value(false));
        }

        @Test
        @DisplayName("기간 모집 공고를 조회한다")
        void getRecruitmentWithPeriod() throws Exception {
            // given
            LocalDateTime startAt = LocalDateTime.now().minusDays(1);
            LocalDateTime endAt = LocalDateTime.now().plusDays(RECRUITMENT_PERIOD_DAYS);
            persist(ClubRecruitmentFixture.createWithPeriod(club, startAt, endAt));
            clearPersistenceContext();

            mockLoginUser(normalUser.getId());

            // when & then
            performGet("/clubs/" + club.getId() + "/recruitments")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONGOING"))
                .andExpect(jsonPath("$.startAt").isNotEmpty())
                .andExpect(jsonPath("$.endAt").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("PUT /clubs/{clubId}/recruitments - 모집 공고 생성/수정")
    class UpsertRecruitment {

        @Test
        @DisplayName("회장이 상시 모집 공고를 생성한다")
        void createAlwaysRecruitmentByPresident() throws Exception {
            // given
            mockLoginUser(president.getId());

            ClubRecruitmentUpsertRequest request = new ClubRecruitmentUpsertRequest(
                null,
                null,
                true,
                "새로운 상시 모집 공고입니다.",
                List.of()
            );

            // when & then
            performPut("/clubs/" + club.getId() + "/recruitments", request)
                .andExpect(status().isNoContent());

            clearPersistenceContext();

            ClubRecruitment saved = clubRecruitmentRepository.getByClubId(club.getId());
            assertThat(saved.getContent()).isEqualTo("새로운 상시 모집 공고입니다.");
            assertThat(saved.getIsAlwaysRecruiting()).isTrue();
        }

        @Test
        @DisplayName("매니저가 기간 모집 공고를 생성한다")
        void createPeriodRecruitmentByManager() throws Exception {
            // given
            mockLoginUser(manager.getId());

            LocalDateTime startAt = LocalDateTime.now().plusDays(1);
            LocalDateTime endAt = LocalDateTime.now().plusDays(RECRUITMENT_PERIOD_DAYS);

            ClubRecruitmentUpsertRequest request = new ClubRecruitmentUpsertRequest(
                startAt,
                endAt,
                false,
                "기간 모집 공고입니다.",
                List.of(
                    new ClubRecruitmentUpsertRequest.InnerClubRecruitmentImageRequest("https://example.com/image1.png"),
                    new ClubRecruitmentUpsertRequest.InnerClubRecruitmentImageRequest("https://example.com/image2.png")
                )
            );

            // when & then
            performPut("/clubs/" + club.getId() + "/recruitments", request)
                .andExpect(status().isNoContent());

            clearPersistenceContext();

            ClubRecruitment saved = clubRecruitmentRepository.getByClubId(club.getId());
            assertThat(saved.getContent()).isEqualTo("기간 모집 공고입니다.");
            assertThat(saved.getIsAlwaysRecruiting()).isFalse();
            assertThat(saved.getStartAt()).isNotNull();
            assertThat(saved.getEndAt()).isNotNull();
            assertThat(saved.getImages()).hasSize(2);
        }

        @Test
        @DisplayName("기존 모집 공고를 수정한다")
        void updateExistingRecruitment() throws Exception {
            // given
            persist(ClubRecruitmentFixture.createAlwaysRecruiting(club));
            clearPersistenceContext();

            mockLoginUser(president.getId());

            LocalDateTime startAt = LocalDateTime.now().plusDays(1);
            LocalDateTime endAt = LocalDateTime.now().plusDays(RECRUITMENT_PERIOD_DAYS);

            ClubRecruitmentUpsertRequest request = new ClubRecruitmentUpsertRequest(
                startAt,
                endAt,
                false,
                "수정된 모집 공고입니다.",
                List.of()
            );

            // when & then
            performPut("/clubs/" + club.getId() + "/recruitments", request)
                .andExpect(status().isNoContent());

            clearPersistenceContext();

            ClubRecruitment updated = clubRecruitmentRepository.getByClubId(club.getId());
            assertThat(updated.getContent()).isEqualTo("수정된 모집 공고입니다.");
            assertThat(updated.getIsAlwaysRecruiting()).isFalse();
        }

        @Test
        @DisplayName("일반 유저가 모집 공고 생성 시 403을 반환한다")
        void createRecruitmentByNormalUserFails() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            ClubRecruitmentUpsertRequest request = new ClubRecruitmentUpsertRequest(
                null,
                null,
                true,
                "모집 공고 내용",
                List.of()
            );

            // when & then
            performPut("/clubs/" + club.getId() + "/recruitments", request)
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("상시 모집에 날짜를 입력하면 400을 반환한다")
        void alwaysRecruitingWithDatesFails() throws Exception {
            // given
            mockLoginUser(president.getId());

            ClubRecruitmentUpsertRequest request = new ClubRecruitmentUpsertRequest(
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(RECRUITMENT_PERIOD_DAYS),
                true,
                "상시 모집인데 날짜가 있음",
                List.of()
            );

            // when & then
            performPut("/clubs/" + club.getId() + "/recruitments", request)
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("기간 모집에 날짜가 없으면 400을 반환한다")
        void periodRecruitingWithoutDatesFails() throws Exception {
            // given
            mockLoginUser(president.getId());

            ClubRecruitmentUpsertRequest request = new ClubRecruitmentUpsertRequest(
                null,
                null,
                false,
                "기간 모집인데 날짜가 없음",
                List.of()
            );

            // when & then
            performPut("/clubs/" + club.getId() + "/recruitments", request)
                .andExpect(status().isBadRequest());
        }
    }
}
