package gg.agit.konect.domain.studytime.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.studytime.model.StudyTimeMonthly;

public interface StudyTimeMonthlyRepository extends Repository<StudyTimeMonthly, Integer> {

    Optional<StudyTimeMonthly> findByUserIdAndStudyMonth(Integer userId, LocalDate studyMonth);

    @Query("""
        SELECT stm
        FROM StudyTimeMonthly stm
        WHERE stm.user.id IN :userIds
        AND stm.studyMonth = :studyMonth
        """)
    List<StudyTimeMonthly> findByUserIds(
        @Param("userIds") List<Integer> userIds,
        @Param("studyMonth") LocalDate studyMonth
    );

    StudyTimeMonthly save(StudyTimeMonthly studyTimeMonthly);

    List<StudyTimeMonthly> findAllByStudyMonth(LocalDate studyMonth);
}
