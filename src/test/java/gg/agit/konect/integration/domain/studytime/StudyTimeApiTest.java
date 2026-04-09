package gg.agit.konect.integration.domain.studytime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import gg.agit.konect.domain.studytime.dto.StudyTimerStopRequest;
import gg.agit.konect.domain.studytime.dto.StudyTimerSyncRequest;
import gg.agit.konect.domain.studytime.model.StudyTimeDaily;
import gg.agit.konect.domain.studytime.model.StudyTimeMonthly;
import gg.agit.konect.domain.studytime.model.StudyTimeTotal;
import gg.agit.konect.domain.studytime.model.StudyTimer;
import gg.agit.konect.domain.studytime.repository.StudyTimeDailyRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeMonthlyRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeTotalRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimerRepository;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class StudyTimeApiTest extends IntegrationTestSupport {

    private static final long MISMATCHED_CLIENT_SECONDS = 100L;

    @Autowired
    private StudyTimerRepository studyTimerRepository;

    @Autowired
    private StudyTimeDailyRepository studyTimeDailyRepository;

    @Autowired
    private StudyTimeMonthlyRepository studyTimeMonthlyRepository;

    @Autowired
    private StudyTimeTotalRepository studyTimeTotalRepository;

    private University university;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        university = persist(UniversityFixture.create());
        user = persist(UserFixture.createUser(university, "테스트유저", "2021136001"));
    }

    @Nested
    @DisplayName("GET /studytimes/summary - 순공 시간 조회")
    class GetSummary {

        @Test
        @DisplayName("순공 시간을 조회한다")
        void getSummarySuccess() throws Exception {
            // given
            mockLoginUser(user.getId());

            // when & then
            performGet("/studytimes/summary")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayStudyTime").value(0))
                .andExpect(jsonPath("$.monthlyStudyTime").value(0))
                .andExpect(jsonPath("$.totalStudyTime").value(0));
        }
    }

    @Nested
    @DisplayName("POST /studytimes/timers - 타이머 시작")
    class StartTimer {

        @Test
        @DisplayName("타이머를 시작한다")
        void startTimerSuccess() throws Exception {
            // given
            mockLoginUser(user.getId());

            // when & then
            performPost("/studytimes/timers")
                .andExpect(status().isOk());

            boolean timerExists = studyTimerRepository.existsByUserId(user.getId());
            assertThat(timerExists).isTrue();
        }

        @Test
        @DisplayName("이미 실행 중인 타이머가 있으면 409를 반환한다")
        void startTimerWhenAlreadyRunningFails() throws Exception {
            // given
            mockLoginUser(user.getId());
            performPost("/studytimes/timers")
                .andExpect(status().isOk());

            // when & then
            performPost("/studytimes/timers")
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("다른 사용자는 독립적으로 타이머를 시작할 수 있다")
        void differentUsersCanStartTimers() throws Exception {
            // given
            User anotherUser = persist(UserFixture.createUser(university, "다른유저", "2021136002"));
            clearPersistenceContext();

            mockLoginUser(user.getId());
            performPost("/studytimes/timers")
                .andExpect(status().isOk());

            mockLoginUser(anotherUser.getId());
            performPost("/studytimes/timers")
                .andExpect(status().isOk());

            // then
            assertThat(studyTimerRepository.existsByUserId(user.getId())).isTrue();
            assertThat(studyTimerRepository.existsByUserId(anotherUser.getId())).isTrue();
        }
    }

    @Nested
    @DisplayName("DELETE /studytimes/timers - 타이머 중지")
    class StopTimer {

        @Test
        @DisplayName("타이머를 중지하면 타이머가 삭제되고 결과가 반환된다")
        void stopTimerSuccess() throws Exception {
            // given
            mockLoginUser(user.getId());
            performPost("/studytimes/timers")
                .andExpect(status().isOk());

            StudyTimerStopRequest request = new StudyTimerStopRequest(0L);

            // when & then
            performDelete("/studytimes/timers", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionSeconds").isNumber())
                .andExpect(jsonPath("$.dailySeconds").isNumber())
                .andExpect(jsonPath("$.monthlySeconds").isNumber())
                .andExpect(jsonPath("$.totalSeconds").isNumber());

            assertThat(studyTimerRepository.existsByUserId(user.getId())).isFalse();
        }

        @Test
        @DisplayName("실행 중인 타이머가 없으면 400을 반환한다")
        void stopTimerWhenNotRunningFails() throws Exception {
            // given
            mockLoginUser(user.getId());
            StudyTimerStopRequest request = new StudyTimerStopRequest(0L);

            // when & then
            performDelete("/studytimes/timers", request)
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("클라이언트 시간과 서버 시간이 크게 차이나면 400을 반환한다")
        void stopTimerWithTimeMismatchFails() throws Exception {
            // given
            mockLoginUser(user.getId());
            performPost("/studytimes/timers")
                .andExpect(status().isOk());

            StudyTimerStopRequest request = new StudyTimerStopRequest(MISMATCHED_CLIENT_SECONDS);

            // when & then
            performDelete("/studytimes/timers", request)
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("타이머 중지 후 시간이 누적된다")
        void stopTimerAccumulatesTime() throws Exception {
            // given
            mockLoginUser(user.getId());
            performPost("/studytimes/timers")
                .andExpect(status().isOk());

            StudyTimer timer = studyTimerRepository.getByUserId(user.getId());
            backdateTimer(timer, 5L, 5L);

            StudyTimerStopRequest request = new StudyTimerStopRequest(5L);

            // when
            performDelete("/studytimes/timers", request)
                .andExpect(status().isOk());

            // then
            clearPersistenceContext();
            StudyTimeDaily daily = studyTimeDailyRepository
                .findByUserIdAndStudyDate(user.getId(), LocalDate.now())
                .orElse(null);
            StudyTimeMonthly monthly = studyTimeMonthlyRepository
                .findByUserIdAndStudyMonth(user.getId(), LocalDate.now().withDayOfMonth(1))
                .orElse(null);
            StudyTimeTotal total = studyTimeTotalRepository.findByUserId(user.getId()).orElse(null);

            assertThat(daily).isNotNull();
            assertThat(daily.getTotalSeconds()).isGreaterThanOrEqualTo(5L);
            assertThat(monthly).isNotNull();
            assertThat(monthly.getTotalSeconds()).isGreaterThanOrEqualTo(5L);
            assertThat(total).isNotNull();
            assertThat(total.getTotalSeconds()).isGreaterThanOrEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("PATCH /studytimes/timers - 타이머 동기화")
    class SyncTimer {

        @Test
        @DisplayName("타이머를 동기화하면 시간이 누적되고 시작 시간이 갱신된다")
        void syncTimerAccumulatesTime() throws Exception {
            // given
            mockLoginUser(user.getId());
            performPost("/studytimes/timers")
                .andExpect(status().isOk());

            StudyTimer timer = studyTimerRepository.getByUserId(user.getId());
            LocalDateTime originalStartedAt = timer.getStartedAt();
            backdateTimer(timer, 5L, 5L);

            StudyTimerSyncRequest request = new StudyTimerSyncRequest(5L);

            // when
            performPatch("/studytimes/timers", request)
                .andExpect(status().isOk());

            // then
            clearPersistenceContext();
            StudyTimer updatedTimer = studyTimerRepository.getByUserId(user.getId());
            assertThat(updatedTimer.getStartedAt()).isAfter(originalStartedAt);

            StudyTimeDaily daily = studyTimeDailyRepository
                .findByUserIdAndStudyDate(user.getId(), LocalDate.now())
                .orElse(null);
            assertThat(daily).isNotNull();
            assertThat(daily.getTotalSeconds()).isGreaterThanOrEqualTo(5L);
        }

        @Test
        @DisplayName("실행 중인 타이머가 없으면 동기화에 실패한다")
        void syncTimerWithoutRunningFails() throws Exception {
            // given
            mockLoginUser(user.getId());
            StudyTimerSyncRequest request = new StudyTimerSyncRequest(0L);

            // when & then
            performPatch("/studytimes/timers", request)
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("클라이언트 시간과 서버 시간이 크게 차이나면 타이머가 삭제된다")
        void syncTimerWithTimeMismatchDeletesTimer() throws Exception {
            // given
            mockLoginUser(user.getId());
            performPost("/studytimes/timers")
                .andExpect(status().isOk());

            StudyTimerSyncRequest request = new StudyTimerSyncRequest(MISMATCHED_CLIENT_SECONDS);

            // when & then
            performPatch("/studytimes/timers", request)
                .andExpect(status().isBadRequest());

            assertThat(studyTimerRepository.existsByUserId(user.getId())).isFalse();
        }

        @Test
        @DisplayName("여러 번 동기화해도 시간이 정확히 누적된다")
        void multipleSyncAccumulatesCorrectly() throws Exception {
            // given
            mockLoginUser(user.getId());
            performPost("/studytimes/timers")
                .andExpect(status().isOk());

            // 첫 번째 동기화
            StudyTimer timer = studyTimerRepository.getByUserId(user.getId());
            backdateTimer(timer, 3L, 3L);

            performPatch("/studytimes/timers", new StudyTimerSyncRequest(3L))
                .andExpect(status().isOk());

            // 두 번째 동기화
            timer = studyTimerRepository.getByUserId(user.getId());
            backdateTimer(timer, 8L, 5L);

            performPatch("/studytimes/timers", new StudyTimerSyncRequest(8L))
                .andExpect(status().isOk());

            // then
            clearPersistenceContext();
            StudyTimeDaily daily = studyTimeDailyRepository
                .findByUserIdAndStudyDate(user.getId(), LocalDate.now())
                .orElse(null);
            assertThat(daily).isNotNull();
            assertThat(daily.getTotalSeconds()).isGreaterThanOrEqualTo(8L);
        }
    }

    @Nested
    @DisplayName("타이머 엣지 케이스")
    class TimerEdgeCases {

        @Test
        @DisplayName("타이머 시작 후 즉시 중지해도 정상 동작한다")
        void stopImmediatelyAfterStart() throws Exception {
            // given
            mockLoginUser(user.getId());
            performPost("/studytimes/timers")
                .andExpect(status().isOk());

            StudyTimerStopRequest request = new StudyTimerStopRequest(0L);

            // when & then
            performDelete("/studytimes/timers", request)
                .andExpect(status().isOk());

            assertThat(studyTimerRepository.existsByUserId(user.getId())).isFalse();
        }

        @Test
        @DisplayName("타이머 시작 후 3초 이내의 시간 차이는 허용된다")
        void timerAllowsSmallTimeDifference() throws Exception {
            // given
            mockLoginUser(user.getId());
            performPost("/studytimes/timers")
                .andExpect(status().isOk());

            StudyTimer timer = studyTimerRepository.getByUserId(user.getId());
            timer.updateStartedAt(LocalDateTime.now().minusSeconds(1));
            persist(timer);
            clearPersistenceContext();

            // 1초 차이는 3초 임계값 이내
            StudyTimerStopRequest request = new StudyTimerStopRequest(1L);

            // when & then
            performDelete("/studytimes/timers", request)
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("0초 동안 타이머를 실행해도 정상 동작한다")
        void timerWithZeroSeconds() throws Exception {
            // given
            mockLoginUser(user.getId());
            performPost("/studytimes/timers")
                .andExpect(status().isOk());

            StudyTimerStopRequest request = new StudyTimerStopRequest(0L);

            // when & then
            performDelete("/studytimes/timers", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionSeconds").value(0));
        }
    }

    private void backdateTimer(StudyTimer timer, long sessionElapsedSeconds, long lastSyncElapsedSeconds) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = now.minusSeconds(sessionElapsedSeconds);
        LocalDateTime startedAt = now.minusSeconds(lastSyncElapsedSeconds);

        // StudyTimerService는 createdAt 기반 totalSeconds를 검증하므로 auditing 컬럼까지 DB에 직접 맞춰둔다.
        entityManager.createNativeQuery("""
                UPDATE study_timer
                SET created_at = :createdAt,
                    started_at = :startedAt,
                    updated_at = :updatedAt
                WHERE id = :id
            """)
            .setParameter("createdAt", createdAt)
            .setParameter("startedAt", startedAt)
            .setParameter("updatedAt", now)
            .setParameter("id", timer.getId())
            .executeUpdate();
        clearPersistenceContext();
    }
}
