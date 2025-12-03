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
import com.querydsl.core.types.dsl.CaseBuilder;
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
                new CaseBuilder()
                    .when(clubFeePayment.status.eq(UNPAID))
                    .then(clubPositionFee.fee)
                    .otherwise(0)
                    .sum()
            )
            .from(clubMember)
            .leftJoin(clubFeePayment)
            .on(clubFeePayment.clubMember.eq(clubMember))
            .leftJoin(clubPositionFee)
            .on(clubPositionFee.clubPosition.eq(clubMember.clubPosition))
            .where(clubMember.id.userId.eq(userId))
            .groupBy(clubMember.club.id)
            .fetch();

        return results.stream()
            .collect(Collectors.toMap(
                tuple -> tuple.get(0, Integer.class),
                tuple -> tuple.get(1, Integer.class)
            ));
    }
}
