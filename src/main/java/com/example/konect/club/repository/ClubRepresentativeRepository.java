package com.example.konect.club.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import com.example.konect.club.model.ClubRepresentative;
import com.example.konect.club.model.ClubMemberId;

public interface ClubRepresentativeRepository extends Repository<ClubRepresentative, ClubMemberId> {

    List<ClubRepresentative> findByClubId(Integer clubId);
}
