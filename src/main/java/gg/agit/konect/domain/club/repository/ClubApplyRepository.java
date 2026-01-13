package gg.agit.konect.domain.club.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.club.model.ClubApply;

public interface ClubApplyRepository extends Repository<ClubApply, Integer> {

    boolean existsByClubIdAndUserId(Integer clubId, Integer userId);

    ClubApply save(ClubApply clubApply);

    void deleteByUserId(Integer userId);

    @Query("""
        SELECT clubApply
        FROM ClubApply clubApply
        JOIN FETCH clubApply.user user
        WHERE clubApply.club.id = :clubId
        """)
    List<ClubApply> findAllByClubIdWithUser(@Param("clubId") Integer clubId);

    @Query("""
        SELECT clubApply
        FROM ClubApply clubApply
        JOIN FETCH clubApply.user user
        WHERE clubApply.club.id = :clubId
          AND clubApply.createdAt BETWEEN :startDateTime AND :endDateTime
        """)
    List<ClubApply> findAllByClubIdAndCreatedAtBetweenWithUser(
        @Param("clubId") Integer clubId,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );
}
