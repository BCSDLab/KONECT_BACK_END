package gg.agit.konect.club.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import gg.agit.konect.club.model.ClubMember;
import gg.agit.konect.club.model.ClubMemberId;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface ClubMemberRepository extends Repository<ClubMember, ClubMemberId> {

    Optional<ClubMember> findByClubIdAndUserId(Integer clubId, Integer userId);

    default ClubMember getByClubIdAndUserId(Integer clubId, Integer userId) {
        return findByClubIdAndUserId(clubId, userId).orElseThrow(() ->
            CustomException.of(ApiResponseCode.NOT_FOUND_CLUB_MEMBER));
    }

    long countByClubId(Integer clubId);

    @Query("""
    SELECT cm FROM ClubMember cm
    JOIN FETCH cm.club c
    JOIN FETCH c.clubCategory
    JOIN FETCH cm.clubPosition cp
    JOIN FETCH cp.clubPositionGroup
    WHERE cm.id.userId = :userId
    """)
    List<ClubMember> findAllByUserId(Integer userId);
}
