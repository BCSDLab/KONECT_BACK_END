package gg.agit.konect.domain.studytime.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.studytime.model.StudyTimeTotal;

public interface StudyTimeTotalRepository extends Repository<StudyTimeTotal, Integer> {

    Optional<StudyTimeTotal> findByUserId(Integer userId);

    StudyTimeTotal save(StudyTimeTotal studyTimeTotal);
}
