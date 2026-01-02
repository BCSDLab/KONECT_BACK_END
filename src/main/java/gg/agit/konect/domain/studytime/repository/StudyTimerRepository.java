package gg.agit.konect.domain.studytime.repository;

import static gg.agit.konect.global.code.ApiResponseCode.STUDY_TIMER_NOT_RUNNING;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.studytime.model.StudyTimer;
import gg.agit.konect.global.exception.CustomException;

public interface StudyTimerRepository extends Repository<StudyTimer, Integer> {

    Optional<StudyTimer> findByUserId(Integer userId);

    default StudyTimer getByUserId(Integer userId) {
        return findByUserId(userId)
            .orElseThrow(() -> CustomException.of(STUDY_TIMER_NOT_RUNNING));
    }

    Boolean existsByUserId(Integer userId);

    StudyTimer save(StudyTimer studyTimer);

    void delete(StudyTimer studyTimer);
}
