package gg.agit.konect.domain.schedule.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.schedule.model.UniversitySchedule;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface UniversityScheduleRepository extends Repository<UniversitySchedule, Integer> {

    void save(UniversitySchedule universitySchedule);

    @Query("""
        SELECT us
        FROM UniversitySchedule us
        JOIN FETCH us.schedule s
        WHERE us.id = :scheduleId
        AND us.university.id = :universityId
        """)
    Optional<UniversitySchedule> findByIdAndUniversityId(
        @Param("scheduleId") Integer scheduleId,
        @Param("universityId") Integer universityId
    );

    default UniversitySchedule getByIdAndUniversityId(Integer scheduleId, Integer universityId) {
        return findByIdAndUniversityId(scheduleId, universityId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_SCHEDULE));
    }
}
