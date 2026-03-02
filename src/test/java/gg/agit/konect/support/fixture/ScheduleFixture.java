package gg.agit.konect.support.fixture;

import java.time.LocalDateTime;

import gg.agit.konect.domain.schedule.model.Schedule;
import gg.agit.konect.domain.schedule.model.ScheduleType;
import gg.agit.konect.domain.schedule.model.UniversitySchedule;
import gg.agit.konect.domain.university.model.University;

public class ScheduleFixture {

    public static Schedule create(String title, LocalDateTime startedAt, LocalDateTime endedAt) {
        return Schedule.of(title, startedAt, endedAt, ScheduleType.UNIVERSITY);
    }

    public static Schedule createUniversity(String title, LocalDateTime startedAt, LocalDateTime endedAt) {
        return Schedule.of(title, startedAt, endedAt, ScheduleType.UNIVERSITY);
    }

    public static Schedule createClub(String title, LocalDateTime startedAt, LocalDateTime endedAt) {
        return Schedule.of(title, startedAt, endedAt, ScheduleType.CLUB);
    }

    public static Schedule createCouncil(String title, LocalDateTime startedAt, LocalDateTime endedAt) {
        return Schedule.of(title, startedAt, endedAt, ScheduleType.COUNCIL);
    }

    public static UniversitySchedule createUniversitySchedule(Schedule schedule, University university) {
        return UniversitySchedule.of(schedule, university);
    }
}
