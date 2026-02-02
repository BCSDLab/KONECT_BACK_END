package gg.agit.konect.domain.schedule.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.schedule.model.Schedule;

public interface ScheduleRepository extends Repository<Schedule, Integer> {

    Optional<Schedule> findById(Integer id);

    Schedule save(Schedule schedule);

    void delete(Schedule schedule);

    @Query("""
        SELECT s
        FROM Schedule s
        LEFT JOIN UniversitySchedule us ON s.id = us.id
        WHERE 1 = CASE
            WHEN us.id IS NOT NULL AND us.university.id = :universityId THEN 1
            ELSE 0
        END
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
        LEFT JOIN UniversitySchedule us ON s.id = us.id
        WHERE 1 = CASE
            WHEN us.id IS NOT NULL AND us.university.id = :universityId THEN 1
            ELSE 0
        END
        AND (s.startedAt < :monthEnd AND s.endedAt > :monthStart)
        ORDER BY s.startedAt ASC
        """)
    List<Schedule> findSchedulesByMonth(
        @Param("universityId") Integer universityId,
        @Param("monthStart") LocalDateTime monthStart,
        @Param("monthEnd") LocalDateTime monthEnd
    );
}
