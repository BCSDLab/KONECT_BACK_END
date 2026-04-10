package gg.agit.konect.domain.studytime.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.studytime.model.StudyTimeDaily;

public interface StudyTimeDailyRepository extends Repository<StudyTimeDaily, Integer> {

    Optional<StudyTimeDaily> findByUserIdAndStudyDate(Integer userId, LocalDate studyDate);

    StudyTimeDaily save(StudyTimeDaily studyTimeDaily);

    @Query("""
        SELECT std
        FROM StudyTimeDaily std
        WHERE std.user.id IN :userIds
        AND std.studyDate = :studyDate
        """)
    List<StudyTimeDaily> findByUserIds(
        @Param("userIds") List<Integer> userIds,
        @Param("studyDate") LocalDate studyDate
    );

    List<StudyTimeDaily> findAllByStudyDate(LocalDate studyDate);

    @Query("""
        SELECT COALESCE(SUM(std.totalSeconds), 0)
        FROM StudyTimeDaily std
        WHERE std.user.id = :userId
        AND std.studyDate BETWEEN :startDate AND :endDate
        """)
    Long sumTotalSecondsByUserIdAndStudyDateBetween(
        @Param("userId") Integer userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT COALESCE(SUM(std.totalSeconds), 0)
        FROM StudyTimeDaily std
        WHERE std.user.id = :userId
        """)
    Long sumTotalSecondsByUserId(@Param("userId") Integer userId);

    @Query("""
        SELECT COALESCE(SUM(std.totalSeconds), 0)
        FROM StudyTimeDaily std
        WHERE std.user.id IN :userIds
        AND std.studyDate = :studyDate
        """)
    Long sumTotalSecondsByUserIdsAndStudyDate(
        @Param("userIds") List<Integer> userIds,
        @Param("studyDate") LocalDate studyDate
    );

    @Query("""
        SELECT COALESCE(SUM(std.totalSeconds), 0)
        FROM StudyTimeDaily std
        WHERE std.user.id IN :userIds
        AND std.studyDate BETWEEN :startDate AND :endDate
        """)
    Long sumTotalSecondsByUserIdsAndStudyDateBetween(
        @Param("userIds") List<Integer> userIds,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
