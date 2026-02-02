package gg.agit.konect.domain.notice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.notice.model.CouncilNoticeReadHistory;

public interface CouncilNoticeReadRepository extends Repository<CouncilNoticeReadHistory, Integer> {

    boolean existsByUserIdAndCouncilNoticeId(Integer userId, Integer councilNoticeId);

    List<CouncilNoticeReadHistory> findByUserIdAndCouncilNoticeIdIn(Integer userId, List<Integer> councilNoticeIds);

    void save(CouncilNoticeReadHistory councilNoticeReadHistory);

    @Query("""
        SELECT COUNT(cn)
        FROM CouncilNotice cn
        WHERE NOT EXISTS (
                SELECT 1
                FROM CouncilNoticeReadHistory cnrh
                WHERE cnrh.councilNotice = cn AND cnrh.user.id = :userId
                )
        """)
    Long countUnreadNoticesByUserId(@Param("userId") Integer userId);
}
