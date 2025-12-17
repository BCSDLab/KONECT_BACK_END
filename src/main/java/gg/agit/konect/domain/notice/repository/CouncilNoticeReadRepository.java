package gg.agit.konect.domain.notice.repository;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.notice.model.CouncilNoticeReadHistory;

public interface CouncilNoticeReadRepository extends Repository<CouncilNoticeReadHistory, Integer> {

    boolean existsByUserIdAndCouncilNoticeId(Integer userId, Integer councilNoticeId);

    void save(CouncilNoticeReadHistory councilNoticeReadHistory);
}
