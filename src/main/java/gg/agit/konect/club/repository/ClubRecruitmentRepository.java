package gg.agit.konect.club.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.club.model.ClubRecruitment;

public interface ClubRecruitmentRepository extends Repository<ClubRecruitment, Integer> {

    Optional<ClubRecruitment> findByClubId(Integer clubId);
}
