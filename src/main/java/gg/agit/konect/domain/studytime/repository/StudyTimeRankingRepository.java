package gg.agit.konect.domain.studytime.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.studytime.model.StudyTimeRanking;
import gg.agit.konect.domain.studytime.model.StudyTimeRankingId;

public interface StudyTimeRankingRepository extends Repository<StudyTimeRanking, StudyTimeRankingId> {

    @Query("""
        SELECT r
        FROM StudyTimeRanking r
        WHERE r.id.rankingTypeId = :rankingTypeId
          AND r.id.universityId = :universityId
        ORDER BY r.dailySeconds DESC, r.monthlySeconds DESC, r.id.targetId ASC
        """)
    Page<StudyTimeRanking> findDailyRankings(
        @Param("rankingTypeId") Integer rankingTypeId,
        @Param("universityId") Integer universityId,
        Pageable pageable
    );

    @Query("""
        SELECT r
        FROM StudyTimeRanking r
        WHERE r.id.rankingTypeId = :rankingTypeId
          AND r.id.universityId = :universityId
        ORDER BY r.monthlySeconds DESC, r.dailySeconds DESC, r.id.targetId ASC
        """)
    Page<StudyTimeRanking> findMonthlyRankings(
        @Param("rankingTypeId") Integer rankingTypeId,
        @Param("universityId") Integer universityId,
        Pageable pageable
    );
}
