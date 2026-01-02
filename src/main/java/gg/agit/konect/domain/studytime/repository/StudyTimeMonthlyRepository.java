package gg.agit.konect.domain.studytime.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.studytime.model.StudyTimeMonthly;

public interface StudyTimeMonthlyRepository extends Repository<StudyTimeMonthly, Integer> {

    Optional<StudyTimeMonthly> findByUserIdAndStudyMonth(Integer userId, LocalDate studyMonth);

    StudyTimeMonthly save(StudyTimeMonthly studyTimeMonthly);
}
