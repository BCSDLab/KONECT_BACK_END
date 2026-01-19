package gg.agit.konect.domain.club.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.club.enums.ClubPositionGroup;
import gg.agit.konect.domain.club.model.ClubPosition;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface ClubPositionRepository extends Repository<ClubPosition, Integer> {

    @Query(value = """
        SELECT cp
        FROM ClubPosition cp
        WHERE cp.id = :id
        """)
    Optional<ClubPosition> findById(@Param(value = "id") Integer id);

    default ClubPosition getById(Integer id) {
        return findById(id).orElseThrow(() ->
            CustomException.of(ApiResponseCode.NOT_FOUND_CLUB_POSITION));
    }

    @Query(value = """
        SELECT cp
        FROM ClubPosition cp
        WHERE cp.club.id = :clubId
        ORDER BY cp.clubPositionGroup
        """)
    List<ClubPosition> findAllByClubId(@Param(value = "clubId") Integer clubId);

    @Query(value = """
        SELECT cp
        FROM ClubPosition cp
        WHERE cp.club.id = :clubId
        AND cp.clubPositionGroup = :positionGroup
        """)
    List<ClubPosition> findAllByClubIdAndPositionGroup(
        @Param(value = "clubId") Integer clubId,
        @Param(value = "positionGroup") ClubPositionGroup positionGroup
    );

    @Query(value = """
        SELECT cp
        FROM ClubPosition cp
        WHERE cp.club.id = :clubId
        AND cp.name = :name
        """)
    Optional<ClubPosition> findByClubIdAndName(
        @Param(value = "clubId") Integer clubId,
        @Param(value = "name") String name
    );

    @Query(value = """
        SELECT cp
        FROM ClubPosition cp
        WHERE cp.club.id = :clubId
        AND cp.clubPositionGroup = :positionGroup
        ORDER BY cp.id
        LIMIT 1
        """)
    Optional<ClubPosition> findFirstByClubIdAndPositionGroup(
        @Param(value = "clubId") Integer clubId,
        @Param(value = "positionGroup") ClubPositionGroup positionGroup
    );

    @Query(value = """
        SELECT COUNT(cp)
        FROM ClubPosition cp
        WHERE cp.club.id = :clubId
        AND cp.clubPositionGroup = :positionGroup
        """)
    long countByClubIdAndPositionGroup(
        @Param(value = "clubId") Integer clubId,
        @Param(value = "positionGroup") ClubPositionGroup positionGroup
    );

    @Query(value = """
        SELECT CASE WHEN COUNT(cp) > 0 THEN true ELSE false END
        FROM ClubPosition cp
        WHERE cp.club.id = :clubId
        AND cp.name = :name
        """)
    boolean existsByClubIdAndName(
        @Param(value = "clubId") Integer clubId,
        @Param(value = "name") String name
    );

    @Query(value = """
        SELECT CASE WHEN COUNT(cp) > 0 THEN true ELSE false END
        FROM ClubPosition cp
        WHERE cp.club.id = :clubId
        AND cp.name = :name
        AND cp.id <> :id
        """)
    boolean existsByClubIdAndNameAndIdNot(
        @Param(value = "clubId") Integer clubId,
        @Param(value = "name") String name,
        @Param(value = "id") Integer id
    );

    ClubPosition save(ClubPosition clubPosition);

    void delete(ClubPosition clubPosition);
}
