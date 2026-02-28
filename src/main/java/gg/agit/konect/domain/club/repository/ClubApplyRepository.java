package gg.agit.konect.domain.club.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.club.model.ClubApply;
import gg.agit.konect.domain.club.enums.ClubApplyStatus;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface ClubApplyRepository extends Repository<ClubApply, Integer> {

    @Query("""
        SELECT COUNT(ca) > 0
        FROM ClubApply ca
        WHERE ca.club.id = :clubId
          AND ca.user.id = :userId
          AND ca.status = gg.agit.konect.domain.club.enums.ClubApplyStatus.PENDING
        """)
    boolean existsByClubIdAndUserId(@Param("clubId") Integer clubId, @Param("userId") Integer userId);

    @Query("""
        SELECT ca.club.id
        FROM ClubApply ca
        WHERE ca.user.id = :userId
          AND ca.club.id IN :clubIds
          AND ca.status = gg.agit.konect.domain.club.enums.ClubApplyStatus.PENDING
        """)
    List<Integer> findClubIdsByUserIdAndClubIdIn(
        @Param("userId") Integer userId,
        @Param("clubIds") List<Integer> clubIds
    );

    ClubApply save(ClubApply clubApply);

    void deleteByUserId(Integer userId);

    @Query("""
        SELECT clubApply
        FROM ClubApply clubApply
        JOIN FETCH clubApply.user user
        WHERE clubApply.id = :id
          AND clubApply.club.id = :clubId
          AND clubApply.status = gg.agit.konect.domain.club.enums.ClubApplyStatus.PENDING
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
          AND clubApply.user.id = :userId
          AND clubApply.status = gg.agit.konect.domain.club.enums.ClubApplyStatus.PENDING
        """)
    Optional<ClubApply> findByClubIdAndUserId(
        @Param("clubId") Integer clubId,
        @Param("userId") Integer userId
    );

    default ClubApply getByClubIdAndUserId(Integer clubId, Integer userId) {
        return findByClubIdAndUserId(clubId, userId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_CLUB_APPLY));
    }

    @Query("""
        SELECT clubApply
        FROM ClubApply clubApply
        JOIN FETCH clubApply.user user
        WHERE clubApply.club.id = :clubId
          AND clubApply.user.id = :userId
          AND clubApply.status = :status
        ORDER BY clubApply.createdAt DESC
        """)
    List<ClubApply> findAllByClubIdAndUserIdAndStatusOrderByCreatedAtDesc(
        @Param("clubId") Integer clubId,
        @Param("userId") Integer userId,
        @Param("status") ClubApplyStatus status
    );

    default ClubApply getLatestApprovedByClubIdAndUserId(Integer clubId, Integer userId) {
        return findAllByClubIdAndUserIdAndStatusOrderByCreatedAtDesc(clubId, userId, ClubApplyStatus.APPROVED).stream()
            .findFirst()
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_CLUB_APPLY));
    }

    @Query("""
        SELECT clubApply
        FROM ClubApply clubApply
        JOIN FETCH clubApply.user user
        WHERE clubApply.club.id = :clubId
          AND clubApply.status = gg.agit.konect.domain.club.enums.ClubApplyStatus.PENDING
        """)
    List<ClubApply> findAllByClubIdWithUser(@Param("clubId") Integer clubId);

    @Query("""
        SELECT clubApply
        FROM ClubApply clubApply
        JOIN FETCH clubApply.club club
        WHERE clubApply.user.id = :userId
          AND clubApply.status = gg.agit.konect.domain.club.enums.ClubApplyStatus.PENDING
          AND NOT EXISTS (
            SELECT 1
            FROM ClubMember clubMember
            WHERE clubMember.club.id = clubApply.club.id
              AND clubMember.user.id = clubApply.user.id
          )
        ORDER BY clubApply.createdAt DESC
        """)
    List<ClubApply> findAllPendingByUserIdWithClub(@Param("userId") Integer userId);

    @Query("""
        SELECT clubApply
        FROM ClubApply clubApply
        JOIN FETCH clubApply.user user
        WHERE clubApply.club.id = :clubId
          AND clubApply.createdAt BETWEEN :startDateTime AND :endDateTime
          AND clubApply.status = gg.agit.konect.domain.club.enums.ClubApplyStatus.PENDING
        """)
    List<ClubApply> findAllByClubIdAndCreatedAtBetweenWithUser(
        @Param("clubId") Integer clubId,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );
}
