package gg.agit.konect.domain.club.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

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

    boolean existsByClubIdAndUserId(Integer clubId, Integer userId);

    List<ClubMember> findByUserId(Integer userId);
}
