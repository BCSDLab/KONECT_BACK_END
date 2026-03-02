package gg.agit.konect.domain.studytime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.studytime.dto.StudyTimerStopRequest;
import gg.agit.konect.domain.studytime.dto.StudyTimerStopResponse;
import gg.agit.konect.domain.studytime.repository.StudyTimeDailyRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeMonthlyRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeTotalRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimerRepository;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

@Transactional
class StudyTimerServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private StudyTimerService studyTimerService;

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
    void setUp() {
        university = persist(UniversityFixture.create());
        user = persist(UserFixture.createUser(university, "테스트유저", "2021136001"));
    }

    @Nested
    @DisplayName("타이머 시작")
    class StartTimer {

        @Test
        @DisplayName("타이머를 시작한다")
        void startTimerSuccess() {
            // when
            studyTimerService.start(user.getId());
            entityManager.flush();

            // then
            boolean timerExists = studyTimerRepository.existsByUserId(user.getId());
            assertThat(timerExists).isTrue();
        }

        @Test
        @DisplayName("이미 실행 중인 타이머가 있으면 예외가 발생한다")
        void startTimerWhenAlreadyRunningFails() {
            // given
            studyTimerService.start(user.getId());
            entityManager.flush();

            // when & then
            assertThatThrownBy(() -> studyTimerService.start(user.getId()))
                .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("다른 사용자는 독립적으로 타이머를 시작할 수 있다")
        void differentUsersCanStartTimers() {
            // given
            User anotherUser = persist(UserFixture.createUser(university, "다른유저", "2021136002"));
            clearPersistenceContext();

            // when
            studyTimerService.start(user.getId());
            studyTimerService.start(anotherUser.getId());
            entityManager.flush();

            // then
            assertThat(studyTimerRepository.existsByUserId(user.getId())).isTrue();
            assertThat(studyTimerRepository.existsByUserId(anotherUser.getId())).isTrue();
        }
    }

    @Nested
    @DisplayName("타이머 중지")
    class StopTimer {

        @Test
        @DisplayName("타이머를 중지하면 타이머가 삭제되고 결과가 반환된다")
        void stopTimerSuccess() {
            // given
            studyTimerService.start(user.getId());
            entityManager.flush();

            StudyTimerStopRequest request = new StudyTimerStopRequest(0L);

            // when
            StudyTimerStopResponse response = studyTimerService.stop(user.getId(), request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.sessionSeconds()).isGreaterThanOrEqualTo(0L);
            assertThat(response.dailySeconds()).isGreaterThanOrEqualTo(0L);
            assertThat(response.monthlySeconds()).isGreaterThanOrEqualTo(0L);
            assertThat(response.totalSeconds()).isGreaterThanOrEqualTo(0L);
            assertThat(studyTimerRepository.existsByUserId(user.getId())).isFalse();
        }

        @Test
        @DisplayName("실행 중인 타이머가 없으면 예외가 발생한다")
        void stopTimerWhenNotRunningFails() {
            // given
            StudyTimerStopRequest request = new StudyTimerStopRequest(0L);

            // when & then
            assertThatThrownBy(() -> studyTimerService.stop(user.getId(), request))
                .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("클라이언트 시간과 서버 시간이 크게 차이나면 예외가 발생한다")
        void stopTimerWithTimeMismatchFails() {
            // given
            studyTimerService.start(user.getId());
            entityManager.flush();

            // 클라이언트가 100초라고 보고 (실제론 0초)
            StudyTimerStopRequest request = new StudyTimerStopRequest(100L);

            // when & then
            assertThatThrownBy(() -> studyTimerService.stop(user.getId(), request))
                .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("공부 시간 조회")
    class GetStudyTime {

        @Autowired
        private StudyTimeQueryService studyTimeQueryService;

        @Test
        @DisplayName("공부 기록이 없으면 0을 반환한다")
        void getStudyTimeWhenNoRecords() {
            // when
            Long dailyTime = studyTimeQueryService.getDailyStudyTime(user.getId());
            Long monthlyTime = studyTimeQueryService.getMonthlyStudyTime(user.getId());
            Long totalTime = studyTimeQueryService.getTotalStudyTime(user.getId());

            // then
            assertThat(dailyTime).isZero();
            assertThat(monthlyTime).isZero();
            assertThat(totalTime).isZero();
        }
    }
}
