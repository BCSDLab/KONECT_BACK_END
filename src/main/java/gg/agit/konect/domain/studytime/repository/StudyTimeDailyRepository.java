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
}
