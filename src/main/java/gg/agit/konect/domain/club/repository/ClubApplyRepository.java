package gg.agit.konect.domain.club.repository;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.club.model.ClubApply;

public interface ClubApplyRepository extends Repository<ClubApply, Integer> {

    boolean existsByClubIdAndUserId(Integer clubId, Integer userId);

    ClubApply save(ClubApply clubApply);
}
