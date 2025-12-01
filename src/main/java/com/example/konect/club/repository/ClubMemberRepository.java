package com.example.konect.club.repository;

import org.springframework.data.repository.Repository;

import com.example.konect.club.model.ClubMember;
import com.example.konect.club.model.ClubMemberId;

public interface ClubMemberRepository extends Repository<ClubMember, ClubMemberId> {

    long countByClubId(Integer clubId);
}
