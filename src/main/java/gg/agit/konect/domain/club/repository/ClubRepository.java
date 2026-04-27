package gg.agit.konect.domain.club.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
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

    @Query(value = """
        SELECT c
        FROM Club c
        LEFT JOIN FETCH c.university
        LEFT JOIN FETCH c.clubRecruitment cr
        WHERE c.id = :id
        """)
    Optional<Club> findByIdWithUniversity(@Param(value = "id") Integer id);

    default Club getById(Integer id) {
        return findById(id).orElseThrow(() ->
            CustomException.of(ApiResponseCode.NOT_FOUND_CLUB));
    }

    default Club getByIdWithUniversity(Integer id) {
        return findByIdWithUniversity(id).orElseThrow(() ->
            CustomException.of(ApiResponseCode.NOT_FOUND_CLUB));
    }

    List<Club> findAll();

    boolean existsById(Integer id);

    @Modifying(flushAutomatically = true)
    @Query(value = """
        UPDATE Club c
        SET c.googleSheetId = :googleSheetId,
            c.sheetColumnMapping = :sheetColumnMapping,
            c.updatedAt = CURRENT_TIMESTAMP
        WHERE c.id = :clubId
        """)
    int updateSheetRegistration(
        @Param(value = "clubId") Integer clubId,
        @Param(value = "googleSheetId") String googleSheetId,
        @Param(value = "sheetColumnMapping") String sheetColumnMapping
    );

    Club save(Club club);

    @Query("SELECT COUNT(c) FROM Club c")
    long countAll();
}
