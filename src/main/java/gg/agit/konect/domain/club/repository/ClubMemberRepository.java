package gg.agit.konect.domain.club.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.club.enums.ClubPositionGroup;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubMemberId;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface ClubMemberRepository extends Repository<ClubMember, ClubMemberId> {

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user
        JOIN FETCH cm.clubPosition cp
        WHERE cm.club.id = :clubId
        ORDER BY 
            CASE cp.clubPositionGroup
                WHEN gg.agit.konect.domain.club.enums.ClubPositionGroup.PRESIDENT THEN 0
                WHEN gg.agit.konect.domain.club.enums.ClubPositionGroup.VICE_PRESIDENT THEN 1
                WHEN gg.agit.konect.domain.club.enums.ClubPositionGroup.MANAGER THEN 2
                WHEN gg.agit.konect.domain.club.enums.ClubPositionGroup.MEMBER THEN 3
            END ASC,
            cp.name ASC
        """)
    List<ClubMember> findAllByClubId(@Param("clubId") Integer clubId);

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user
        JOIN FETCH cm.clubPosition cp
        WHERE cm.club.id = :clubId
        AND cp.clubPositionGroup = :positionGroup
        ORDER BY cp.name ASC
        """)
    List<ClubMember> findAllByClubIdAndPositionGroup(
        @Param("clubId") Integer clubId,
        @Param("positionGroup") ClubPositionGroup positionGroup
    );

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.club c
        JOIN FETCH cm.clubPosition cp
        WHERE cm.id.userId = :userId
        """)
    List<ClubMember> findAllByUserId(Integer userId);

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user
        JOIN FETCH cm.clubPosition cp
        WHERE cm.club.id = :clubId
        AND cp.name = '회장'
        """)
    Optional<ClubMember> findPresidentByClubId(@Param("clubId") Integer clubId);

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user
        JOIN FETCH cm.clubPosition cp
        WHERE cm.user.id = :userId
        AND cp.clubPositionGroup = :clubPositionGroup
        """)
    List<ClubMember> findAllByUserIdAndClubPosition(
        @Param("userId") Integer userId,
        @Param("clubPositionGroup") ClubPositionGroup clubPositionGroup
    );

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.club c
        JOIN FETCH cm.clubPosition cp
        WHERE cm.id.userId = :userId
        AND cp.clubPositionGroup IN :clubPositionGroups
        """)
    List<ClubMember> findAllByUserIdAndClubPositionGroups(
        @Param("userId") Integer userId,
        @Param("clubPositionGroups") Set<ClubPositionGroup> clubPositionGroups
    );

    @Query("""
        SELECT COUNT(cm) > 0
        FROM ClubMember cm
        JOIN cm.clubPosition cp
        WHERE cm.club.id = :clubId
        AND cm.user.id = :userId
        AND cp.clubPositionGroup IN :positionGroups
        """)
    boolean existsByClubIdAndUserIdAndPositionGroupIn(
        @Param("clubId") Integer clubId,
        @Param("userId") Integer userId,
        @Param("positionGroups") Set<ClubPositionGroup> positionGroups
    );

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user
        JOIN FETCH cm.clubPosition
        WHERE cm.club.id = :clubId
        AND cm.user.id = :userId
        """)
    Optional<ClubMember> findByClubIdAndUserId(@Param("clubId") Integer clubId, @Param("userId") Integer userId);

    default ClubMember getByClubIdAndUserId(Integer clubId, Integer userId) {
        return findByClubIdAndUserId(clubId, userId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_CLUB_MEMBER));
    }

    boolean existsByClubIdAndUserId(Integer clubId, Integer userId);

    @Query("""
        SELECT cm.id.clubId
        FROM ClubMember cm
        WHERE cm.id.userId = :userId
          AND cm.id.clubId IN :clubIds
        """)
    List<Integer> findClubIdsByUserIdAndClubIdIn(
        @Param("userId") Integer userId,
        @Param("clubIds") List<Integer> clubIds
    );

    List<ClubMember> findByUserId(Integer userId);

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user
        WHERE cm.club.id IN :clubIds
        """)
    List<ClubMember> findByClubIdIn(@Param("clubIds") List<Integer> clubIds);

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.club c
        JOIN FETCH c.university
        WHERE cm.id.userId IN :userIds
        """)
    List<ClubMember> findByUserIdIn(@Param("userIds") List<Integer> userIds);

    @Query("""
        SELECT COUNT(cm)
        FROM ClubMember cm
        WHERE cm.clubPosition.id = :positionId
        """)
    long countByPositionId(@Param("positionId") Integer positionId);

    @Query("""
        SELECT COUNT(cm)
        FROM ClubMember cm
        JOIN cm.clubPosition cp
        WHERE cm.club.id = :clubId
        AND cp.clubPositionGroup = :positionGroup
        """)
    long countByClubIdAndPositionGroup(
        @Param("clubId") Integer clubId,
        @Param("positionGroup") ClubPositionGroup positionGroup
    );

    void delete(ClubMember clubMember);

    ClubMember save(ClubMember clubMember);

    void deleteByUserId(Integer userId);

}
