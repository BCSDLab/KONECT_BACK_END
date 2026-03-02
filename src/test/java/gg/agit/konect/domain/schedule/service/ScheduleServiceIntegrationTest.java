package gg.agit.konect.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.schedule.dto.ScheduleCondition;
import gg.agit.konect.domain.schedule.dto.SchedulesResponse;
import gg.agit.konect.domain.schedule.model.Schedule;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ScheduleFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

@Transactional
class ScheduleServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ScheduleService scheduleService;

    private University university;
    private User user;

    @BeforeEach
    void setUp() {
        university = persist(UniversityFixture.create());
        user = persist(UserFixture.createUser(university, "테스트유저", "2021136001"));
    }

    @Nested
    @DisplayName("다가오는 일정 조회")
    class GetUpcomingSchedules {

        @Test
        @DisplayName("다가오는 일정 3개를 조회한다")
        void getUpcomingSchedulesSuccess() {
            // given
            LocalDateTime now = LocalDateTime.now();

            Schedule schedule1 = persist(ScheduleFixture.createUniversity(
                "수강신청", now.plusDays(1), now.plusDays(2)
            ));
            Schedule schedule2 = persist(ScheduleFixture.createUniversity(
                "중간고사", now.plusDays(5), now.plusDays(10)
            ));
            Schedule schedule3 = persist(ScheduleFixture.createUniversity(
                "기말고사", now.plusDays(30), now.plusDays(35)
            ));

            persist(ScheduleFixture.createUniversitySchedule(schedule1, university));
            persist(ScheduleFixture.createUniversitySchedule(schedule2, university));
            persist(ScheduleFixture.createUniversitySchedule(schedule3, university));
            clearPersistenceContext();

            // when
            SchedulesResponse response = scheduleService.getUpcomingSchedules(user.getId());

            // then
            assertThat(response.schedules()).hasSize(3);
            assertThat(response.schedules().get(0).title()).isEqualTo("수강신청");
        }

        @Test
        @DisplayName("종료된 일정은 다가오는 일정에 포함되지 않는다")
        void pastSchedulesNotIncluded() {
            // given
            LocalDateTime now = LocalDateTime.now();

            Schedule pastSchedule = persist(ScheduleFixture.createUniversity(
                "지난 일정", now.minusDays(10), now.minusDays(5)
            ));
            Schedule futureSchedule = persist(ScheduleFixture.createUniversity(
                "미래 일정", now.plusDays(1), now.plusDays(5)
            ));

            persist(ScheduleFixture.createUniversitySchedule(pastSchedule, university));
            persist(ScheduleFixture.createUniversitySchedule(futureSchedule, university));
            clearPersistenceContext();

            // when
            SchedulesResponse response = scheduleService.getUpcomingSchedules(user.getId());

            // then
            assertThat(response.schedules()).hasSize(1);
            assertThat(response.schedules().get(0).title()).isEqualTo("미래 일정");
        }

        @Test
        @DisplayName("다른 대학의 일정은 조회되지 않는다")
        void otherUniversitySchedulesNotIncluded() {
            // given
            University otherUniversity = persist(UniversityFixture.createWithName("다른대학교"));
            LocalDateTime now = LocalDateTime.now();

            Schedule mySchedule = persist(ScheduleFixture.createUniversity(
                "우리대학 일정", now.plusDays(1), now.plusDays(5)
            ));
            Schedule otherSchedule = persist(ScheduleFixture.createUniversity(
                "다른대학 일정", now.plusDays(2), now.plusDays(6)
            ));

            persist(ScheduleFixture.createUniversitySchedule(mySchedule, university));
            persist(ScheduleFixture.createUniversitySchedule(otherSchedule, otherUniversity));
            clearPersistenceContext();

            // when
            SchedulesResponse response = scheduleService.getUpcomingSchedules(user.getId());

            // then
            assertThat(response.schedules()).hasSize(1);
            assertThat(response.schedules().get(0).title()).isEqualTo("우리대학 일정");
        }
    }

    @Nested
    @DisplayName("월별 일정 조회")
    class GetSchedules {

        @Test
        @DisplayName("특정 월의 일정을 조회한다")
        void getSchedulesByMonthSuccess() {
            // given
            LocalDateTime marchStart = LocalDateTime.of(2026, 3, 1, 0, 0);

            Schedule marchSchedule = persist(ScheduleFixture.createUniversity(
                "3월 일정", marchStart.plusDays(5), marchStart.plusDays(10)
            ));
            Schedule aprilSchedule = persist(ScheduleFixture.createUniversity(
                "4월 일정", LocalDateTime.of(2026, 4, 5, 0, 0), LocalDateTime.of(2026, 4, 10, 0, 0)
            ));

            persist(ScheduleFixture.createUniversitySchedule(marchSchedule, university));
            persist(ScheduleFixture.createUniversitySchedule(aprilSchedule, university));
            clearPersistenceContext();

            ScheduleCondition condition = new ScheduleCondition(2026, 3, null);

            // when
            SchedulesResponse response = scheduleService.getSchedules(condition, user.getId());

            // then
            assertThat(response.schedules()).hasSize(1);
            assertThat(response.schedules().get(0).title()).isEqualTo("3월 일정");
        }

        @Test
        @DisplayName("검색어로 일정을 필터링한다")
        void getSchedulesWithQuery() {
            // given
            LocalDateTime marchStart = LocalDateTime.of(2026, 3, 1, 0, 0);

            Schedule schedule1 = persist(ScheduleFixture.createUniversity(
                "수강신청 기간", marchStart.plusDays(1), marchStart.plusDays(3)
            ));
            Schedule schedule2 = persist(ScheduleFixture.createUniversity(
                "중간고사", marchStart.plusDays(10), marchStart.plusDays(15)
            ));

            persist(ScheduleFixture.createUniversitySchedule(schedule1, university));
            persist(ScheduleFixture.createUniversitySchedule(schedule2, university));
            clearPersistenceContext();

            ScheduleCondition condition = new ScheduleCondition(2026, 3, "수강");

            // when
            SchedulesResponse response = scheduleService.getSchedules(condition, user.getId());

            // then
            assertThat(response.schedules()).hasSize(1);
            assertThat(response.schedules().get(0).title()).isEqualTo("수강신청 기간");
        }

        @Test
        @DisplayName("월을 걸치는 일정도 조회된다")
        void getSchedulesSpanningMonths() {
            // given
            Schedule spanningSchedule = persist(ScheduleFixture.createUniversity(
                "장기 일정",
                LocalDateTime.of(2026, 2, 25, 0, 0),
                LocalDateTime.of(2026, 3, 5, 0, 0)
            ));

            persist(ScheduleFixture.createUniversitySchedule(spanningSchedule, university));
            clearPersistenceContext();

            ScheduleCondition condition = new ScheduleCondition(2026, 3, null);

            // when
            SchedulesResponse response = scheduleService.getSchedules(condition, user.getId());

            // then
            assertThat(response.schedules()).hasSize(1);
        }
    }
}
