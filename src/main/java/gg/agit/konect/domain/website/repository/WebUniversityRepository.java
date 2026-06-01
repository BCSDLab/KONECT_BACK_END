package gg.agit.konect.domain.website.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.website.model.WebUniversity;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface WebUniversityRepository extends Repository<WebUniversity, Integer> {

    Optional<WebUniversity> findById(Integer id);

    default WebUniversity getById(Integer id) {
        return findById(id).orElseThrow(() ->
            CustomException.of(ApiResponseCode.UNIVERSITY_NOT_FOUND));
    }
}
