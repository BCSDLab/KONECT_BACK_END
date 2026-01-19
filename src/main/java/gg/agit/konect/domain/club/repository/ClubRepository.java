package gg.agit.konect.domain.club.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface ClubRepository extends Repository<Club, Integer> {

    @Query(value = """
        SELECT c
        FROM Club c
        LEFT JOIN FETCH c.clubRecruitment cr
        WHERE c.id = :id
        """)
    Optional<Club> findById(@Param(value = "id") Integer id);

    default Club getById(Integer id) {
        return findById(id).orElseThrow(() ->
            CustomException.of(ApiResponseCode.NOT_FOUND_CLUB));
    }

    Club save(Club club);
}
