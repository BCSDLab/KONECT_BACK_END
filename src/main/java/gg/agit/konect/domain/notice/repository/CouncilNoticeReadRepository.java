package gg.agit.konect.domain.notice.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.notice.model.CouncilNoticeReadHistory;

public interface CouncilNoticeReadRepository extends Repository<CouncilNoticeReadHistory, Integer> {

    boolean existsByUserIdAndCouncilNoticeId(Integer userId, Integer councilNoticeId);

    List<CouncilNoticeReadHistory> findByUserIdAndCouncilNoticeIdIn(Integer userId, List<Integer> councilNoticeIds);

    void save(CouncilNoticeReadHistory councilNoticeReadHistory);
}
