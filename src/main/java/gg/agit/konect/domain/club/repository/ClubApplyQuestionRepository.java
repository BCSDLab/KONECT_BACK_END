package gg.agit.konect.domain.club.repository;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.club.model.ClubApplyQuestion;

public interface ClubApplyQuestionRepository extends Repository<ClubApplyQuestion, Integer> {

    @Query("""
        SELECT question
        FROM ClubApplyQuestion question
        WHERE question.club.id = :clubId
          AND question.deletedAt IS NULL
        ORDER BY question.id ASC
        """)
    List<ClubApplyQuestion> findAllByClubIdOrderByIdAsc(@Param("clubId") Integer clubId);

    @Query("""
        SELECT question
        FROM ClubApplyQuestion question
        WHERE question.club.id = :clubId
          AND question.createdAt <= :appliedAt
          AND (question.deletedAt IS NULL OR question.deletedAt > :appliedAt)
        ORDER BY question.id ASC
        """)
    List<ClubApplyQuestion> findAllVisibleAtApplyTime(
        @Param("clubId") Integer clubId,
        @Param("appliedAt") LocalDateTime appliedAt
    );

    ClubApplyQuestion save(ClubApplyQuestion question);

    List<ClubApplyQuestion> saveAll(Iterable<ClubApplyQuestion> questions);
}
