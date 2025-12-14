package gg.agit.konect.domain.council.repository;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_COUNCIL;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.council.model.Council;
import gg.agit.konect.global.exception.CustomException;

public interface CouncilRepository extends Repository<Council, Integer> {

    Optional<Council> findById(Integer id);

    default Council getById(Integer id) {
        return findById(id).orElseThrow(() ->
            CustomException.of(NOT_FOUND_COUNCIL));
    }

    void deleteById(Integer id);

    void save(Council council);
}
