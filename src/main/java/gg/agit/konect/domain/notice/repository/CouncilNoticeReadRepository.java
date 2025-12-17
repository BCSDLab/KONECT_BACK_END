package gg.agit.konect.domain.notice.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.notice.model.CouncilNoticeReadHistory;

public interface CouncilNoticeReadRepository extends Repository<CouncilNoticeReadHistory, Integer> {

    Optional<CouncilNoticeReadHistory> findByUserId(Integer userId);

    void save(CouncilNoticeReadHistory councilNoticeReadHistory);
}
