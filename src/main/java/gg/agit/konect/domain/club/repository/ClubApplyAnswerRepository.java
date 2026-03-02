package gg.agit.konect.domain.club.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.club.model.ClubApplyAnswer;

public interface ClubApplyAnswerRepository extends Repository<ClubApplyAnswer, Integer> {

    List<ClubApplyAnswer> saveAll(Iterable<ClubApplyAnswer> answers);

    @Query("""
        SELECT answer
        FROM ClubApplyAnswer answer
        JOIN FETCH answer.question question
        WHERE answer.apply.id = :applyId
        ORDER BY question.displayOrder ASC, question.id ASC
        """)
    List<ClubApplyAnswer> findAllByApplyIdWithQuestion(@Param("applyId") Integer applyId);
}
