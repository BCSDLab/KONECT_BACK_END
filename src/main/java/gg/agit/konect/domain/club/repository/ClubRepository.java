package gg.agit.konect.domain.club.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface ClubRepository extends Repository<Club, Integer> {

    Optional<Club> findById(Integer id);

    default Club getById(Integer id) {
        return findById(id).orElseThrow(() ->
            CustomException.of(ApiResponseCode.NOT_FOUND_CLUB));
    }
}
