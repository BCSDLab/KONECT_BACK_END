package gg.agit.konect.domain.studytime.repository;

import java.util.List;
import java.util.Optional;

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

    @Query("""
        SELECT r
        FROM StudyTimeRanking r
        WHERE r.id.rankingTypeId = :rankingTypeId
          AND r.id.universityId = :universityId
          AND r.id.targetId = :targetId
        """)
    Optional<StudyTimeRanking> findRanking(
        @Param("rankingTypeId") Integer rankingTypeId,
        @Param("universityId") Integer universityId,
        @Param("targetId") Integer targetId
    );

    @Query("""
        SELECT r
        FROM StudyTimeRanking r
        WHERE r.id.rankingTypeId = :rankingTypeId
          AND r.id.universityId = :universityId
          AND r.targetName = :targetName
        """)
    Optional<StudyTimeRanking> findRankingByName(
        @Param("rankingTypeId") Integer rankingTypeId,
        @Param("universityId") Integer universityId,
        @Param("targetName") String targetName
    );

    @Query("""
        SELECT COUNT(r)
        FROM StudyTimeRanking r
        WHERE r.id.rankingTypeId = :rankingTypeId
          AND r.id.universityId = :universityId
          AND (
            r.dailySeconds > :dailySeconds
            OR (
                r.dailySeconds = :dailySeconds
                AND r.monthlySeconds > :monthlySeconds
            )
            OR (
                r.dailySeconds = :dailySeconds
                AND r.monthlySeconds = :monthlySeconds
                AND r.id.targetId < :targetId
            )
          )
        """)
    long countDailyHigherRankings(
        @Param("rankingTypeId") Integer rankingTypeId,
        @Param("universityId") Integer universityId,
        @Param("dailySeconds") Long dailySeconds,
        @Param("monthlySeconds") Long monthlySeconds,
        @Param("targetId") Integer targetId
    );

    @Query("""
        SELECT COUNT(r)
        FROM StudyTimeRanking r
        WHERE r.id.rankingTypeId = :rankingTypeId
          AND r.id.universityId = :universityId
          AND (
            r.monthlySeconds > :monthlySeconds
            OR (
                r.monthlySeconds = :monthlySeconds
                AND r.dailySeconds > :dailySeconds
            )
            OR (
                r.monthlySeconds = :monthlySeconds
                AND r.dailySeconds = :dailySeconds
                AND r.id.targetId < :targetId
            )
          )
        """)
    long countMonthlyHigherRankings(
        @Param("rankingTypeId") Integer rankingTypeId,
        @Param("universityId") Integer universityId,
        @Param("monthlySeconds") Long monthlySeconds,
        @Param("dailySeconds") Long dailySeconds,
        @Param("targetId") Integer targetId
    );

    @Query("""
        SELECT COALESCE(MAX(r.id.targetId), 0)
        FROM StudyTimeRanking r
        WHERE r.id.rankingTypeId = :rankingTypeId
          AND r.id.universityId = :universityId
        """)
    Integer findMaxTargetId(
        @Param("rankingTypeId") Integer rankingTypeId,
        @Param("universityId") Integer universityId
    );

    List<StudyTimeRanking> findAll();

    void save(StudyTimeRanking studyTimeRanking);
}
