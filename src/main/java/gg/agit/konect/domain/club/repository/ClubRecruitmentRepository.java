package gg.agit.konect.domain.club.repository;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CLUB_RECRUITMENT;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.global.exception.CustomException;

public interface ClubRecruitmentRepository extends Repository<ClubRecruitment, Integer> {

    Optional<ClubRecruitment> findByClubId(Integer clubId);

    default ClubRecruitment getByClubId(Integer clubId) {
        return findByClubId(clubId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_RECRUITMENT));
    }
}
