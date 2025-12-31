package gg.agit.konect.domain.studytime.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.studytime.model.StudyTimeDaily;

public interface StudyTimeDailyRepository extends Repository<StudyTimeDaily, Integer> {

    Optional<StudyTimeDaily> findByUserIdAndStudyDate(Integer userId, LocalDate studyDate);

    StudyTimeDaily save(StudyTimeDaily studyTimeDaily);
}
