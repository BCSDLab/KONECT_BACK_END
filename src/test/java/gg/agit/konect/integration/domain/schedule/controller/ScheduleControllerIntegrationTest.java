package gg.agit.konect.integration.domain.schedule.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.domain.schedule.model.Schedule;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ScheduleFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class ScheduleControllerIntegrationTest extends IntegrationTestSupport {

    private static final int TEST_YEAR = 2026;
    private static final int MARCH = 3;
    private static final int APRIL = 4;
    private static final int FEBRUARY = 2;
    private static final int EXPECTED_SCHEDULE_COUNT = 3;
    private static final int DAYS_5 = 5;
    private static final int DAYS_6 = 6;
    private static final int DAYS_10 = 10;
    private static final int DAYS_15 = 15;
    private static final int DAYS_25 = 25;
    private static final int DAYS_30 = 30;
    private static final int DAYS_35 = 35;

    private University university;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        university = persist(UniversityFixture.create());
        user = persist(UserFixture.createUser(university, "테스트유저", "2021136001"));
    }

    @Nested
    @DisplayName("GET /schedules/upcoming - 다가오는 일정 조회")
    class GetUpcomingSchedules {

        @Test
        @DisplayName("다가오는 일정 3개를 조회한다")
        void getUpcomingSchedulesSuccess() throws Exception {
            // given
            LocalDateTime now = LocalDateTime.now();

            Schedule schedule1 = persist(ScheduleFixture.createUniversity(
                "수강신청", now.plusDays(1), now.plusDays(2)
            ));
            Schedule schedule2 = persist(ScheduleFixture.createUniversity(
                "중간고사", now.plusDays(DAYS_5), now.plusDays(DAYS_10)
            ));
            Schedule schedule3 = persist(ScheduleFixture.createUniversity(
                "기말고사", now.plusDays(DAYS_30), now.plusDays(DAYS_35)
            ));

            persist(ScheduleFixture.createUniversitySchedule(schedule1, university));
            persist(ScheduleFixture.createUniversitySchedule(schedule2, university));
            persist(ScheduleFixture.createUniversitySchedule(schedule3, university));
            clearPersistenceContext();

            mockLoginUser(user.getId());

            // when & then
            performGet("/schedules/upcoming")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedules", hasSize(EXPECTED_SCHEDULE_COUNT)))
                .andExpect(jsonPath("$.schedules[0].title").value("수강신청"));
        }

        @Test
        @DisplayName("종료된 일정은 다가오는 일정에 포함되지 않는다")
        void pastSchedulesNotIncluded() throws Exception {
            // given
            LocalDateTime now = LocalDateTime.now();

            Schedule pastSchedule = persist(ScheduleFixture.createUniversity(
                "지난 일정", now.minusDays(DAYS_10), now.minusDays(DAYS_5)
            ));
            Schedule futureSchedule = persist(ScheduleFixture.createUniversity(
                "미래 일정", now.plusDays(1), now.plusDays(DAYS_5)
            ));

            persist(ScheduleFixture.createUniversitySchedule(pastSchedule, university));
            persist(ScheduleFixture.createUniversitySchedule(futureSchedule, university));
            clearPersistenceContext();

            mockLoginUser(user.getId());

            // when & then
            performGet("/schedules/upcoming")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedules", hasSize(1)))
                .andExpect(jsonPath("$.schedules[0].title").value("미래 일정"));
        }

        @Test
        @DisplayName("다른 대학의 일정은 조회되지 않는다")
        void otherUniversitySchedulesNotIncluded() throws Exception {
            // given
            University otherUniversity = persist(UniversityFixture.createWithName("다른대학교"));
            LocalDateTime now = LocalDateTime.now();

            Schedule mySchedule = persist(ScheduleFixture.createUniversity(
                "우리대학 일정", now.plusDays(1), now.plusDays(DAYS_5)
            ));
            Schedule otherSchedule = persist(ScheduleFixture.createUniversity(
                "다른대학 일정", now.plusDays(2), now.plusDays(DAYS_6)
            ));

            persist(ScheduleFixture.createUniversitySchedule(mySchedule, university));
            persist(ScheduleFixture.createUniversitySchedule(otherSchedule, otherUniversity));
            clearPersistenceContext();

            mockLoginUser(user.getId());

            // when & then
            performGet("/schedules/upcoming")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedules", hasSize(1)))
                .andExpect(jsonPath("$.schedules[0].title").value("우리대학 일정"));
        }
    }

    @Nested
    @DisplayName("GET /schedules - 월별 일정 조회")
    class GetSchedules {

        @Test
        @DisplayName("특정 월의 일정을 조회한다")
        void getSchedulesByMonthSuccess() throws Exception {
            // given
            LocalDateTime marchStart = LocalDateTime.of(TEST_YEAR, MARCH, 1, 0, 0);

            Schedule marchSchedule = persist(ScheduleFixture.createUniversity(
                "3월 일정", marchStart.plusDays(DAYS_5), marchStart.plusDays(DAYS_10)
            ));
            Schedule aprilSchedule = persist(ScheduleFixture.createUniversity(
                "4월 일정",
                LocalDateTime.of(TEST_YEAR, APRIL, DAYS_5, 0, 0),
                LocalDateTime.of(TEST_YEAR, APRIL, DAYS_10, 0, 0)
            ));

            persist(ScheduleFixture.createUniversitySchedule(marchSchedule, university));
            persist(ScheduleFixture.createUniversitySchedule(aprilSchedule, university));
            clearPersistenceContext();

            mockLoginUser(user.getId());

            // when & then
            performGet("/schedules?year=" + TEST_YEAR + "&month=" + MARCH)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedules", hasSize(1)))
                .andExpect(jsonPath("$.schedules[0].title").value("3월 일정"));
        }

        @Test
        @DisplayName("검색어로 일정을 필터링한다")
        void getSchedulesWithQuery() throws Exception {
            // given
            LocalDateTime marchStart = LocalDateTime.of(TEST_YEAR, MARCH, 1, 0, 0);

            Schedule schedule1 = persist(ScheduleFixture.createUniversity(
                "수강신청 기간", marchStart.plusDays(1), marchStart.plusDays(EXPECTED_SCHEDULE_COUNT)
            ));
            Schedule schedule2 = persist(ScheduleFixture.createUniversity(
                "중간고사", marchStart.plusDays(DAYS_10), marchStart.plusDays(DAYS_15)
            ));

            persist(ScheduleFixture.createUniversitySchedule(schedule1, university));
            persist(ScheduleFixture.createUniversitySchedule(schedule2, university));
            clearPersistenceContext();

            mockLoginUser(user.getId());

            // when & then
            performGet("/schedules?year=" + TEST_YEAR + "&month=" + MARCH + "&query=수강")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedules", hasSize(1)))
                .andExpect(jsonPath("$.schedules[0].title").value("수강신청 기간"));
        }

        @Test
        @DisplayName("월을 걸치는 일정도 조회된다")
        void getSchedulesSpanningMonths() throws Exception {
            // given
            Schedule spanningSchedule = persist(ScheduleFixture.createUniversity(
                "장기 일정",
                LocalDateTime.of(TEST_YEAR, FEBRUARY, DAYS_25, 0, 0),
                LocalDateTime.of(TEST_YEAR, MARCH, DAYS_5, 0, 0)
            ));

            persist(ScheduleFixture.createUniversitySchedule(spanningSchedule, university));
            clearPersistenceContext();

            mockLoginUser(user.getId());

            // when & then
            performGet("/schedules?year=" + TEST_YEAR + "&month=" + MARCH)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedules", hasSize(1)));
        }
    }
}
