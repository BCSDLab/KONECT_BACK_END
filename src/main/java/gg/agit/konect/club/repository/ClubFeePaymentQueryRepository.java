package gg.agit.konect.club.repository;

import static gg.agit.konect.club.enums.FeePaymentStatus.UNPAID;
import static gg.agit.konect.club.model.QClubFeePayment.clubFeePayment;
import static gg.agit.konect.club.model.QClubMember.clubMember;
import static gg.agit.konect.club.model.QClubPositionFee.clubPositionFee;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClubFeePaymentQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Map<Integer, Integer> findUnpaidFeeAmountByUserId(Integer userId) {
        List<Tuple> results = jpaQueryFactory
            .select(
                clubMember.club.id,
                clubPositionFee.fee.sum().coalesce(0)
            )
            .from(clubFeePayment)
            .join(clubFeePayment.clubMember, clubMember)
            .leftJoin(clubPositionFee)
            .on(clubPositionFee.clubPosition.id.eq(clubMember.clubPosition.id))
            .where(
                clubMember.id.userId.eq(userId),
                clubFeePayment.status.eq(UNPAID)
            )
            .groupBy(clubMember.club.id)
            .fetch();

        return results.stream()
            .collect(Collectors.toMap(
                tuple -> tuple.get(0, Integer.class),
                tuple -> tuple.get(1, Integer.class)
            ));
    }
}
