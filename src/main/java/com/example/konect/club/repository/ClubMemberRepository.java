package com.example.konect.club.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.example.konect.club.model.ClubMember;
import com.example.konect.club.model.ClubMemberId;
import com.example.konect.global.code.ApiResponseCode;
import com.example.konect.global.exception.CustomException;

public interface ClubMemberRepository extends Repository<ClubMember, ClubMemberId> {

    Optional<ClubMember> findByClubIdAndUserId(Integer clubId, Integer userId);

    default ClubMember getByClubIdAndUserId(Integer clubId, Integer userId) {
        return findByClubIdAndUserId(clubId, userId).orElseThrow(() ->
            CustomException.of(ApiResponseCode.NOT_FOUND_CLUB_MEMBER));
    }

    long countByClubId(Integer clubId);
}
