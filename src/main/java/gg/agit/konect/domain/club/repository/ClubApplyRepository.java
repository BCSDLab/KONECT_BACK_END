package gg.agit.konect.domain.club.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.club.model.ClubApply;

public interface ClubApplyRepository extends Repository<ClubApply, Integer> {

    boolean existsByClubIdAndUserId(Integer clubId, Integer userId);

    ClubApply save(ClubApply clubApply);

    void deleteByUserId(Integer userId);

    List<ClubApply> findAllByClubId(Integer clubId);

    List<ClubApply> findAllByClubIdAndCreatedAtBetween(
        Integer clubId,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
    );
}
