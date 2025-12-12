package gg.agit.konect.university.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.university.model.University;

public interface UniversityRepository extends Repository<University, Integer> {

    Optional<University> findByKoreanName(String koreanName);
}
