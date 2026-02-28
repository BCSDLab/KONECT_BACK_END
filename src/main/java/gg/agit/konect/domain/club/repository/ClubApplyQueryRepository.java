package gg.agit.konect.domain.club.repository;

import static gg.agit.konect.domain.club.model.QClubApply.clubApply;
import static gg.agit.konect.domain.club.model.QClubMember.clubMember;
import static gg.agit.konect.domain.user.model.QUser.user;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import gg.agit.konect.domain.club.dto.ClubApplicationCondition;
import gg.agit.konect.domain.club.enums.ClubApplyStatus;
import gg.agit.konect.domain.club.enums.ClubApplicationSortBy;
import gg.agit.konect.domain.club.model.ClubApply;
import gg.agit.konect.domain.club.model.QClubApply;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClubApplyQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Page<ClubApply> findAllByClubId(
        Integer clubId,
        ClubApplicationCondition condition
    ) {
        PageRequest pageable = PageRequest.of(condition.page() - 1, condition.limit());
        OrderSpecifier<?> orderSpecifier = createOrderSpecifier(
            condition.sortBy(),
            condition.sortDirection()
        );

        BooleanExpression notAlreadyMember = notAlreadyClubMember(clubId);
        BooleanExpression activeUserOnly = user.deletedAt.isNull();
        BooleanExpression pendingOnly = clubApply.status.eq(ClubApplyStatus.PENDING);

        List<ClubApply> content = jpaQueryFactory
            .selectFrom(clubApply)
            .join(clubApply.user, user).fetchJoin()
            .where(
                clubApply.club.id.eq(clubId),
                pendingOnly,
                activeUserOnly,
                notAlreadyMember
            )
            .orderBy(orderSpecifier)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = jpaQueryFactory
            .select(clubApply.count())
            .from(clubApply)
            .where(
                clubApply.club.id.eq(clubId),
                pendingOnly,
                activeUserOnly,
                notAlreadyMember
            )
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    public Page<ClubApply> findAllByClubIdAndCreatedAtBetween(
        Integer clubId,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        ClubApplicationCondition condition
    ) {
        PageRequest pageable = PageRequest.of(condition.page() - 1, condition.limit());
        OrderSpecifier<?> orderSpecifier = createOrderSpecifier(
            condition.sortBy(),
            condition.sortDirection()
        );

        BooleanExpression notAlreadyMember = notAlreadyClubMember(clubId);
        BooleanExpression activeUserOnly = user.deletedAt.isNull();
        BooleanExpression pendingOnly = clubApply.status.eq(ClubApplyStatus.PENDING);

        List<ClubApply> content = jpaQueryFactory
            .selectFrom(clubApply)
            .join(clubApply.user, user).fetchJoin()
            .where(
                clubApply.club.id.eq(clubId),
                clubApply.createdAt.between(startDateTime, endDateTime),
                pendingOnly,
                activeUserOnly,
                notAlreadyMember
            )
            .orderBy(orderSpecifier)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = jpaQueryFactory
            .select(clubApply.count())
            .from(clubApply)
            .where(
                clubApply.club.id.eq(clubId),
                clubApply.createdAt.between(startDateTime, endDateTime),
                pendingOnly,
                activeUserOnly,
                notAlreadyMember
            )
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private OrderSpecifier<?> createOrderSpecifier(
        ClubApplicationSortBy sortBy,
        Sort.Direction sortDirection
    ) {
        boolean isAsc = sortDirection.isAscending();
        return switch (sortBy) {
            case APPLIED_AT -> isAsc
                ? clubApply.createdAt.asc()
                : clubApply.createdAt.desc();
            case STUDENT_NUMBER -> isAsc
                ? user.studentNumber.asc()
                : user.studentNumber.desc();
            case NAME -> isAsc
                ? user.name.asc()
                : user.name.desc();
        };
    }

    private BooleanExpression notAlreadyClubMember(Integer clubId) {
        return JPAExpressions
            .selectOne()
            .from(clubMember)
            .where(
                clubMember.club.id.eq(clubId),
                clubMember.user.id.eq(clubApply.user.id)
            )
            .notExists();
    }

    public Page<ClubApply> findApprovedMemberApplicationsByClubId(
        Integer clubId,
        ClubApplicationCondition condition
    ) {
        PageRequest pageable = PageRequest.of(condition.page() - 1, condition.limit());
        OrderSpecifier<?> orderSpecifier = createOrderSpecifier(
            condition.sortBy(),
            condition.sortDirection()
        );

        BooleanExpression isClubMember = isAlreadyClubMember(clubId);
        BooleanExpression activeUserOnly = user.deletedAt.isNull();
        BooleanExpression approvedOnly = clubApply.status.eq(ClubApplyStatus.APPROVED);
        BooleanExpression latestApprovedApplicationOnly = isLatestApprovedApplicationByUser(clubId);

        List<ClubApply> content = jpaQueryFactory
            .selectFrom(clubApply)
            .join(clubApply.user, user).fetchJoin()
            .where(
                clubApply.club.id.eq(clubId),
                activeUserOnly,
                isClubMember,
                approvedOnly,
                latestApprovedApplicationOnly
            )
            .orderBy(orderSpecifier)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = jpaQueryFactory
            .select(clubApply.count())
            .from(clubApply)
            .where(
                clubApply.club.id.eq(clubId),
                activeUserOnly,
                isClubMember,
                approvedOnly,
                latestApprovedApplicationOnly
            )
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private BooleanExpression isAlreadyClubMember(Integer clubId) {
        return JPAExpressions
            .selectOne()
            .from(clubMember)
            .where(
                clubMember.club.id.eq(clubId),
                clubMember.user.id.eq(clubApply.user.id)
            )
            .exists();
    }

    private BooleanExpression isLatestApprovedApplicationByUser(Integer clubId) {
        QClubApply latestApply = new QClubApply("latestApply");

        return clubApply.id.eq(
            JPAExpressions
                .select(latestApply.id.max())
                .from(latestApply)
                .where(
                    latestApply.club.id.eq(clubId),
                    latestApply.status.eq(ClubApplyStatus.APPROVED),
                    latestApply.user.id.eq(clubApply.user.id)
                )
        );
    }
}
