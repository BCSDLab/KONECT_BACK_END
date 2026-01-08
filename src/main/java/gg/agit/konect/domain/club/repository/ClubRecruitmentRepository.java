package gg.agit.konect.domain.club.repository;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CLUB_RECRUITMENT;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.global.exception.CustomException;

public interface ClubRecruitmentRepository extends Repository<ClubRecruitment, Integer> {

    @Query("""
        SELECT c
        FROM ClubRecruitment c
        LEFT JOIN FETCH c.images i
        WHERE c.club.id = :clubId
        ORDER BY i.displayOrder ASC
        """)
    Optional<ClubRecruitment> findByClubId(@Param("clubId") Integer clubId);

    default ClubRecruitment getByClubId(Integer clubId) {
        return findByClubId(clubId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_RECRUITMENT));
    }
}
