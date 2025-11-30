package com.example.konect.club.repository;

import org.springframework.data.repository.Repository;

import com.example.konect.club.model.ClubMember;

public interface ClubMemberRepository extends Repository<ClubMember, Integer> {

    long countByClubId(Integer clubId);
}
