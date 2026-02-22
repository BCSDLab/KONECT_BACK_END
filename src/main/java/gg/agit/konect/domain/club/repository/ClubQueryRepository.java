package gg.agit.konect.domain.club.repository;

import static gg.agit.konect.domain.club.model.QClub.club;
import static gg.agit.konect.domain.club.model.QClubRecruitment.clubRecruitment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import gg.agit.konect.domain.club.enums.RecruitmentStatus;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.model.ClubSummaryInfo;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClubQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Page<ClubSummaryInfo> findAllByFilter(
        PageRequest pageable, String query, Boolean isRecruiting, Integer universityId
    ) {
        BooleanBuilder condition = createClubSearchCondition(query, isRecruiting, universityId);
        List<OrderSpecifier<?>> orders = createClubSortOrders(isRecruiting);

        List<Club> clubs = fetchClubs(pageable, condition, orders);
        List<ClubSummaryInfo> content = convertToSummaryInfo(clubs);
        Long total = countClubs(condition, isRecruiting);

        return new PageImpl<>(content, pageable, total);
    }

    /*      쿼리       */
    private List<Club> fetchClubs(PageRequest pageable, BooleanBuilder condition, List<OrderSpecifier<?>> orders) {
        return jpaQueryFactory.select(club)
            .from(club)
            .leftJoin(club.clubRecruitment, clubRecruitment).fetchJoin()
            .where(condition)
            .orderBy(orders.toArray(new OrderSpecifier[0]))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
    }

    private Long countClubs(BooleanBuilder condition, Boolean isRecruiting) {
        JPAQuery<Long> query = jpaQueryFactory
            .select(club.countDistinct())
            .from(club);

        if (isRecruiting) {
            query.leftJoin(clubRecruitment).on(clubRecruitment.club.id.eq(club.id));
        }

        return query.where(condition).fetchOne();
    }

    /*      검색 조건       */
    private BooleanBuilder createClubSearchCondition(String query, Boolean isRecruiting, Integer universityId) {
        BooleanBuilder condition = new BooleanBuilder();

        addUniversityCondition(condition, universityId);
        addQuerySearchCondition(condition, query);
        addRecruitingCondition(condition, isRecruiting);

        return condition;
    }

    private void addUniversityCondition(BooleanBuilder condition, Integer universityId) {
        condition.and(club.university.id.eq(universityId));
    }

    private void addQuerySearchCondition(BooleanBuilder condition, String query) {
        if (StringUtils.isEmpty(query)) {
            return;
        }

        String normalizedQuery = query.trim().toLowerCase();
        BooleanBuilder searchCondition = new BooleanBuilder();
        searchCondition.or(club.name.lower().contains(normalizedQuery));

        condition.and(searchCondition);
    }

    private void addRecruitingCondition(BooleanBuilder condition, Boolean isRecruiting) {
        if (!isRecruiting) {
            return;
        }

        condition.and(createOngoingRecruitmentCondition());
    }

    private BooleanExpression createOngoingRecruitmentCondition() {
        LocalDateTime now = LocalDateTime.now();
        return club.isRecruitmentEnabled.isTrue()
            .and(clubRecruitment.id.isNotNull())
            .and(
                clubRecruitment.isAlwaysRecruiting.isTrue()
                    .or(clubRecruitment.startAt.loe(now).and(clubRecruitment.endAt.goe(now)))
            );
    }

    /*      정렬 조건       */
    private List<OrderSpecifier<?>> createClubSortOrders(Boolean isRecruiting) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        addRecruitmentSortOrder(orders, isRecruiting);
        addDefaultSortOrder(orders);

        return orders;
    }

    private void addRecruitmentSortOrder(List<OrderSpecifier<?>> orders, Boolean isRecruiting) {
        BooleanExpression isOngoingRecruitment = createOngoingRecruitmentCondition();

        if (!isRecruiting) {
            orders.add(
                new CaseBuilder()
                    .when(isOngoingRecruitment)
                    .then(0)
                    .otherwise(1)
                    .asc()
            );
        }

        orders.add(
            new CaseBuilder()
                .when(isOngoingRecruitment.and(clubRecruitment.endAt.isNull()))
                .then(1)
                .otherwise(0)
                .asc()
        );

        orders.add(
            new CaseBuilder()
                .when(isOngoingRecruitment)
                .then(clubRecruitment.endAt)
                .otherwise((LocalDateTime)null)
                .asc()
        );
    }

    private void addDefaultSortOrder(List<OrderSpecifier<?>> orders) {
        orders.add(club.id.asc());
    }

    /*      DTO 변환      */
    private List<ClubSummaryInfo> convertToSummaryInfo(List<Club> clubs) {
        return clubs.stream()
            .map(club -> {
                ClubRecruitment recruitment = club.getClubRecruitment();
                boolean isRecruitmentEnabled = Boolean.TRUE.equals(club.getIsRecruitmentEnabled());
                RecruitmentStatus status = isRecruitmentEnabled
                    ? RecruitmentStatus.of(recruitment)
                    : RecruitmentStatus.CLOSED;

                boolean isAlwaysRecruiting = isRecruitmentEnabled
                    && recruitment != null
                    && Boolean.TRUE.equals(recruitment.getIsAlwaysRecruiting());
                LocalDateTime applicationDeadline = (recruitment != null && !isAlwaysRecruiting)
                    ? recruitment.getEndAt()
                    : null;

                return new ClubSummaryInfo(
                    club.getId(),
                    club.getName(),
                    club.getImageUrl(),
                    club.getClubCategory().getDescription(),
                    club.getDescription(),
                    status,
                    isAlwaysRecruiting,
                    applicationDeadline
                );
            })
            .toList();
    }
}
