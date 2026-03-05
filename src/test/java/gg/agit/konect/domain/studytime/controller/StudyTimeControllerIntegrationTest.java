package gg.agit.konect.domain.studytime.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import gg.agit.konect.domain.studytime.dto.StudyTimerStopRequest;
import gg.agit.konect.domain.studytime.repository.StudyTimerRepository;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.ControllerTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class StudyTimeControllerIntegrationTest extends ControllerTestSupport {

    private static final long MISMATCHED_CLIENT_SECONDS = 100L;

    @Autowired
    private StudyTimerRepository studyTimerRepository;

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
    }
}
