package gg.agit.konect.domain.university.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.university.model.University;

public interface UniversityRepository extends Repository<University, Integer> {

    Optional<University> findById(Integer id);

    List<University> findAllByOrderByKoreanNameAsc();
}
