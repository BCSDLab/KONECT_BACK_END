package com.example.konect.club.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import com.example.konect.club.model.ClubExecutive;
import com.example.konect.club.model.ClubMemberId;

public interface ClubExecutiveRepository extends Repository<ClubExecutive, ClubMemberId> {

    List<ClubExecutive> findByClubIdAndIsRepresentative(Integer clubId, Boolean isRepresentative);
}
