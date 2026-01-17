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

public interface ClubMemberRepository extends Repository<ClubMember, ClubMemberId> {

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user
        JOIN FETCH cm.clubPosition
        WHERE cm.club.id = :clubId
        """)
    List<ClubMember> findAllByClubId(@Param("clubId") Integer clubId);

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
        JOIN FETCH cm.clubPosition
        WHERE cm.club.id = :clubId
        AND cm.user.id = :userId
        """)
    Optional<ClubMember> findByClubIdAndUserId(@Param("clubId") Integer clubId, @Param("userId") Integer userId);

    boolean existsByClubIdAndUserId(Integer clubId, Integer userId);

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

    ClubMember save(ClubMember clubMember);

    void deleteByUserId(Integer userId);
}
