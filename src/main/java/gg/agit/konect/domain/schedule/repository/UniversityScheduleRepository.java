package gg.agit.konect.domain.schedule.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.schedule.model.UniversitySchedule;

public interface UniversityScheduleRepository extends Repository<UniversitySchedule, Integer> {

    List<UniversitySchedule> findByUniversityIdAndStartedAtGreaterThanEqualOrderByStartedAtAsc(
        Integer universityId,
        LocalDate startedAt
    );
}
