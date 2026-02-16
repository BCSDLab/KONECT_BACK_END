package gg.agit.konect.domain.club.repository;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubMemberId;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface ClubMemberRepository extends Repository<ClubMember, ClubMemberId> {

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user
        WHERE cm.club.id = :clubId
        ORDER BY
            CASE cm.clubPosition
                WHEN gg.agit.konect.domain.club.enums.ClubPosition.PRESIDENT THEN 0
                WHEN gg.agit.konect.domain.club.enums.ClubPosition.VICE_PRESIDENT THEN 1
                WHEN gg.agit.konect.domain.club.enums.ClubPosition.MANAGER THEN 2
                WHEN gg.agit.konect.domain.club.enums.ClubPosition.MEMBER THEN 3
            END ASC,
            cm.user.name ASC
        """)
    List<ClubMember> findAllByClubId(@Param("clubId") Integer clubId);

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user
        WHERE cm.club.id = :clubId
        AND cm.clubPosition = :position
        ORDER BY cm.user.name ASC
        """)
    List<ClubMember> findAllByClubIdAndPosition(
        @Param("clubId") Integer clubId,
        @Param("position") ClubPosition position
    );

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.club c
        WHERE cm.id.userId = :userId
        """)
    List<ClubMember> findAllByUserId(Integer userId);

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user
        WHERE cm.club.id = :clubId
        AND cm.clubPosition = gg.agit.konect.domain.club.enums.ClubPosition.PRESIDENT
        """)
    Optional<ClubMember> findPresidentByClubId(@Param("clubId") Integer clubId);

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user
        WHERE cm.user.id = :userId
        AND cm.clubPosition = :clubPosition
        """)
    List<ClubMember> findAllByUserIdAndClubPosition(
        @Param("userId") Integer userId,
        @Param("clubPosition") ClubPosition clubPosition
    );

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.club c
        WHERE cm.id.userId = :userId
        AND cm.clubPosition IN :clubPositions
        """)
    List<ClubMember> findAllByUserIdAndClubPositions(
        @Param("userId") Integer userId,
        @Param("clubPositions") Set<ClubPosition> clubPositions
    );

    @Query("""
        SELECT COUNT(cm) > 0
        FROM ClubMember cm
        WHERE cm.club.id = :clubId
        AND cm.user.id = :userId
        AND cm.clubPosition IN :positions
        """)
    boolean existsByClubIdAndUserIdAndPositionIn(
        @Param("clubId") Integer clubId,
        @Param("userId") Integer userId,
        @Param("positions") Set<ClubPosition> positions
    );

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user
        WHERE cm.club.id = :clubId
        AND cm.user.id = :userId
        """)
    Optional<ClubMember> findByClubIdAndUserId(@Param("clubId") Integer clubId, @Param("userId") Integer userId);

    @Query("""
        SELECT cm.createdAt
        FROM ClubMember cm
        WHERE cm.club.id = :clubId
        AND cm.user.id = :userId
        """)
    Optional<LocalDateTime> findJoinedAtByClubIdAndUserId(
        @Param("clubId") Integer clubId,
        @Param("userId") Integer userId
    );

    default LocalDateTime getJoinedAtByClubIdAndUserId(Integer clubId, Integer userId) {
        return findJoinedAtByClubIdAndUserId(clubId, userId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_CLUB_MEMBER));
    }

    default ClubMember getByClubIdAndUserId(Integer clubId, Integer userId) {
        return findByClubIdAndUserId(clubId, userId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_CLUB_MEMBER));
    }

    boolean existsByClubIdAndUserId(Integer clubId, Integer userId);

    @Query("""
        SELECT cm.id.clubId
        FROM ClubMember cm
        WHERE cm.id.userId = :userId
          AND cm.id.clubId IN :clubIds
        """)
    List<Integer> findClubIdsByUserIdAndClubIdIn(
        @Param("userId") Integer userId,
        @Param("clubIds") List<Integer> clubIds
    );

    List<ClubMember> findByUserId(Integer userId);

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user
        WHERE cm.club.id IN :clubIds
        """)
    List<ClubMember> findByClubIdIn(@Param("clubIds") List<Integer> clubIds);

    @Query("""
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.club c
        JOIN FETCH c.university
        WHERE cm.id.userId IN :userIds
        """)
    List<ClubMember> findByUserIdIn(@Param("userIds") List<Integer> userIds);

    @Query("""
        SELECT cm.user.id
        FROM ClubMember cm
        WHERE cm.club.id = :clubId
        """)
    List<Integer> findUserIdsByClubId(@Param("clubId") Integer clubId);

    long countByClubId(Integer clubId);

    @Query("""
        SELECT COUNT(cm)
        FROM ClubMember cm
        WHERE cm.club.id = :clubId
        AND cm.clubPosition = :position
        """)
    long countByClubIdAndPosition(
        @Param("clubId") Integer clubId,
        @Param("position") ClubPosition position
    );

    void delete(ClubMember clubMember);

    ClubMember save(ClubMember clubMember);

    void deleteByUserId(Integer userId);

}
