package com.example.konect.club.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.example.konect.club.model.ClubExecutive;
import com.example.konect.club.model.ClubMemberId;

public interface ClubExecutiveRepository extends Repository<ClubExecutive, ClubMemberId> {

    Optional<ClubExecutive> findByClubIdAndIsRepresentative(Integer clubId, Boolean isRepresentative);

    default ClubExecutive getByClubIdAndIsRepresentative(Integer clubId, Boolean isRepresentative) {
        return findByClubIdAndIsRepresentative(clubId, isRepresentative).orElseThrow(() ->
            new RuntimeException("동아리 대표 임원진이 존재하지 않습니다."));
    }
}
