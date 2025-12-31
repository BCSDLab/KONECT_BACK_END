package gg.agit.konect.domain.schedule.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.schedule.model.Schedule;

public interface ScheduleRepository extends Repository<Schedule, Integer> {

    @Query("""
        SELECT s
        FROM Schedule s
        JOIN UniversitySchedule us ON s.id = us.id
        WHERE us.university.id = :universityId
        AND s.endedAt >= :today
        ORDER BY s.startedAt ASC
        """)
    List<Schedule> findUpcomingSchedules(
        @Param("universityId") Integer universityId,
        @Param("today") LocalDateTime today,
        Pageable pageable
    );

    @Query("""
        SELECT s
        FROM Schedule s
        JOIN UniversitySchedule us ON s.id = us.id
        WHERE us.university.id = :universityId
        AND (
            (s.startedAt >= :monthStart AND s.startedAt < :monthEnd)
            OR (s.endedAt >= :monthStart AND s.endedAt < :monthEnd)
            OR (s.startedAt < :monthStart AND s.endedAt >= :monthEnd)
        )
        ORDER BY s.startedAt ASC
        """)
    List<Schedule> findSchedulesByMonth(
        @Param("universityId") Integer universityId,
        @Param("monthStart") LocalDateTime monthStart,
        @Param("monthEnd") LocalDateTime monthEnd
    );
}
