package gg.agit.konect.domain.club.repository;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CLUB_POSITION;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.club.enums.ClubPositionGroup;
import gg.agit.konect.domain.club.model.ClubPosition;
import gg.agit.konect.global.exception.CustomException;

public interface ClubPositionRepository extends Repository<ClubPosition, Integer> {

    ClubPosition save(ClubPosition clubPosition);

    void delete(ClubPosition clubPosition);

    Optional<ClubPosition> findById(Integer id);

    default ClubPosition getById(Integer id) {
        return findById(id)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_POSITION));
    }

    @Query("""
        SELECT cp
        FROM ClubPosition cp
        WHERE cp.club.id = :clubId
        ORDER BY 
            CASE cp.clubPositionGroup
                WHEN gg.agit.konect.domain.club.enums.ClubPositionGroup.PRESIDENT THEN 0
                WHEN gg.agit.konect.domain.club.enums.ClubPositionGroup.VICE_PRESIDENT THEN 1
                WHEN gg.agit.konect.domain.club.enums.ClubPositionGroup.MANAGER THEN 2
                WHEN gg.agit.konect.domain.club.enums.ClubPositionGroup.MEMBER THEN 3
            END ASC,
            cp.name ASC
        """)
    List<ClubPosition> findAllByClubId(@Param("clubId") Integer clubId);

    boolean existsByClubIdAndName(Integer clubId, String name);

    boolean existsByClubIdAndNameAndIdNot(Integer clubId, String name, Integer id);

    long countByClubIdAndClubPositionGroup(Integer clubId, ClubPositionGroup positionGroup);

    Optional<ClubPosition> findFirstByClubIdAndClubPositionGroup(Integer clubId, ClubPositionGroup positionGroup);

    default ClubPosition getFirstByClubIdAndClubPositionGroup(Integer clubId, ClubPositionGroup positionGroup) {
        return findFirstByClubIdAndClubPositionGroup(clubId, positionGroup)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_POSITION));
    }
}
