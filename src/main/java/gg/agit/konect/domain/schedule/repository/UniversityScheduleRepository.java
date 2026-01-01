package gg.agit.konect.domain.schedule.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.schedule.model.UniversitySchedule;

public interface UniversityScheduleRepository extends Repository<UniversitySchedule, Integer> {

    @Query("""
        SELECT us FROM UniversitySchedule us
        WHERE us.university.id = :universityId
        AND us.endedAt >= :today
        ORDER BY us.startedAt ASC
        """)
    List<UniversitySchedule> findUpcomingSchedules(
        @Param("universityId") Integer universityId,
        @Param("today") LocalDateTime today
    );

    @Query("""
        SELECT us
        FROM UniversitySchedule us
        WHERE us.university.id = :universityId
        AND us.endedAt >= :today
        ORDER BY us.startedAt ASC
        """)
    List<UniversitySchedule> findUpcomingSchedulesWithLimit(
        @Param("universityId") Integer universityId,
        @Param("today") LocalDateTime today,
        Pageable pageable
    );

    @Query("""
        SELECT us
        FROM UniversitySchedule us
        WHERE us.university.id = :universityId
        AND (us.startedAt < :monthEnd AND us.endedAt > :monthStart)
        ORDER BY us.startedAt ASC
        """)
    List<UniversitySchedule> findSchedulesByMonth(
        @Param("universityId") Integer universityId,
        @Param("monthStart") LocalDateTime monthStart,
        @Param("monthEnd") LocalDateTime monthEnd
    );
}
