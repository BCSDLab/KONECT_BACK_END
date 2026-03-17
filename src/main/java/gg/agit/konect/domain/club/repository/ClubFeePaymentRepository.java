package gg.agit.konect.domain.club.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.club.model.ClubFeePayment;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface ClubFeePaymentRepository extends Repository<ClubFeePayment, Integer> {

    ClubFeePayment save(ClubFeePayment clubFeePayment);

    @Query("""
        SELECT fp
        FROM ClubFeePayment fp
        JOIN FETCH fp.user
        WHERE fp.club.id = :clubId
        AND fp.user.id = :userId
        """)
    Optional<ClubFeePayment> findByClubIdAndUserId(
        @Param("clubId") Integer clubId,
        @Param("userId") Integer userId
    );

    default ClubFeePayment getByClubIdAndUserId(Integer clubId, Integer userId) {
        return findByClubIdAndUserId(clubId, userId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_FEE_PAYMENT));
    }

    @Query("""
        SELECT fp
        FROM ClubFeePayment fp
        JOIN FETCH fp.user
        WHERE fp.club.id = :clubId
        """)
    List<ClubFeePayment> findAllByClubId(@Param("clubId") Integer clubId);
}
