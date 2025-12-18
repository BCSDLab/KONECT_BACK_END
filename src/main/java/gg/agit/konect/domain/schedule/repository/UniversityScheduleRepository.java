package gg.agit.konect.domain.schedule.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.schedule.model.UniversitySchedule;

public interface UniversityScheduleRepository extends Repository<UniversitySchedule, Integer> {

    @Query("""
        SELECT us FROM UniversitySchedule us
        WHERE us.university.id = :universityId
        AND us.endedDate >= :today
        ORDER BY us.startedDate ASC, us.startedTime ASC
        """)
    List<UniversitySchedule> findUpcomingSchedules(
        @Param("universityId") Integer universityId,
        @Param("today") LocalDate today
    );
}
