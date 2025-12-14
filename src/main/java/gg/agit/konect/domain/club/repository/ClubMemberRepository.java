package gg.agit.konect.domain.club.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubMemberId;

public interface ClubMemberRepository extends Repository<ClubMember, ClubMemberId> {

    List<ClubMember> findAllByClubId(Integer clubId);

    @Query("""
        SELECT cm FROM ClubMember cm
        JOIN FETCH cm.club c
        JOIN FETCH cm.clubPosition cp
        WHERE cm.id.userId = :userId
        """)
    List<ClubMember> findAllByUserId(Integer userId);
}
