package gg.agit.konect.domain.club.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.club.model.ClubApply;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface ClubApplyRepository extends Repository<ClubApply, Integer> {

    boolean existsByClubIdAndUserId(Integer clubId, Integer userId);

    ClubApply save(ClubApply clubApply);

    void deleteByUserId(Integer userId);

    @Query("""
        SELECT clubApply
        FROM ClubApply clubApply
        JOIN FETCH clubApply.user user
        WHERE clubApply.id = :id
          AND clubApply.club.id = :clubId
        """)
    Optional<ClubApply> findByIdAndClubId(
        @Param("id") Integer id,
        @Param("clubId") Integer clubId
    );

    default ClubApply getByIdAndClubId(Integer id, Integer clubId) {
        return findByIdAndClubId(id, clubId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_CLUB_APPLY));
    }

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
