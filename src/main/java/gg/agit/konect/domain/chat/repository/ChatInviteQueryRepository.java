package gg.agit.konect.domain.chat.repository;

import static gg.agit.konect.domain.club.model.QClub.club;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import gg.agit.konect.domain.chat.model.QChatRoomMember;
import gg.agit.konect.domain.club.model.QClub;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.QClubMember;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.QUser;
import gg.agit.konect.domain.user.model.User;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ChatInviteQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Page<User> findInvitableUsers(Integer userId, String query, PageRequest pageRequest) {
        QChatRoomMember requesterMember = new QChatRoomMember("requesterMember");
        QChatRoomMember candidateMember = new QChatRoomMember("candidateMember");
        QUser candidateUser = new QUser("candidateUser");

        List<User> content = createInvitableUsersQuery(userId, query, requesterMember, candidateMember, candidateUser)
            .orderBy(
                candidateUser.name.asc(),
                candidateUser.studentNumber.asc(),
                candidateUser.id.asc()
            )
            .offset(pageRequest.getOffset())
            .limit(pageRequest.getPageSize())
            .fetch();

        Long total = createInvitableUsersCountQuery(userId, query, requesterMember, candidateMember, candidateUser)
            .fetchOne();

        return new PageImpl<>(content, pageRequest, total == null ? 0 : total);
    }

    public List<User> findInvitableUsers(Integer userId, String query) {
        // 같은 chat_room_member 테이블을 두 번 조인하는 self join 이라 별도 alias가 필요하다.
        QChatRoomMember requesterMember = new QChatRoomMember("requesterMember");
        QChatRoomMember candidateMember = new QChatRoomMember("candidateMember");
        QUser candidateUser = new QUser("candidateUser");

        return createInvitableUsersQuery(userId, query, requesterMember, candidateMember, candidateUser)
            .orderBy(
                candidateUser.name.asc(),
                candidateUser.studentNumber.asc(),
                candidateUser.id.asc()
            )
            .fetch();
    }

    public Page<Integer> findInvitableUserIdsGroupedByClub(
        Integer userId,
        String query,
        PageRequest pageRequest
    ) {
        QChatRoomMember requesterMember = new QChatRoomMember("requesterMember");
        QChatRoomMember candidateMember = new QChatRoomMember("candidateMember");
        QUser candidateUser = new QUser("candidateUser");
        QClubMember requesterClubMember = new QClubMember("requesterClubMember");
        QClubMember candidateClubMember = new QClubMember("candidateClubMember");
        QClub sharedClub = new QClub("sharedClub");

        StringExpression representativeClubName = new CaseBuilder()
            .when(requesterClubMember.id.isNotNull())
            .then(sharedClub.name)
            .otherwise((String)null);
        NumberExpression<Integer> clubPresenceOrder = new CaseBuilder()
            .when(requesterClubMember.id.isNotNull())
            .then(0)
            .otherwise(1);

        List<Integer> content = createInvitableUsersQuery(
            userId,
            query,
            requesterMember,
            candidateMember,
            candidateUser
        )
            // 사용자가 여러 동아리에 속해도 대표 정렬 키는
            // 요청자와 실제로 공유하는 동아리만 기준으로 계산해야 한다.
            .leftJoin(candidateClubMember)
            .on(candidateClubMember.user.id.eq(candidateUser.id))
            .leftJoin(candidateClubMember.club, sharedClub)
            .leftJoin(requesterClubMember)
            .on(
                requesterClubMember.club.id.eq(sharedClub.id)
                    .and(requesterClubMember.user.id.eq(userId))
            )
            .groupBy(
                candidateUser.id,
                candidateUser.name,
                candidateUser.imageUrl,
                candidateUser.studentNumber
            )
            .orderBy(
                // 공유 동아리가 있는 사용자를 먼저 두고,
                // 그 안에서는 대표 동아리 이름 → 사용자 이름 순으로 페이지 경계를 고정한다.
                clubPresenceOrder.min().asc(),
                representativeClubName.min().asc(),
                candidateUser.name.asc(),
                candidateUser.studentNumber.asc(),
                candidateUser.id.asc()
            )
            .select(candidateUser.id)
            .offset(pageRequest.getOffset())
            .limit(pageRequest.getPageSize())
            .fetch();

        Long total = createInvitableUsersCountQuery(userId, query, requesterMember, candidateMember, candidateUser)
            .fetchOne();

        return new PageImpl<>(content, pageRequest, total == null ? 0 : total);
    }

    private JPAQuery<User> createInvitableUsersQuery(
        Integer userId,
        String query,
        QChatRoomMember requesterMember,
        QChatRoomMember candidateMember,
        QUser candidateUser
    ) {
        return jpaQueryFactory.select(candidateUser)
            .from(requesterMember)
            .join(candidateMember)
            .on(candidateMember.chatRoom.id.eq(requesterMember.chatRoom.id))
            .join(candidateMember.user, candidateUser)
            .where(
                requesterMember.user.id.eq(userId),
                requesterMember.leftAt.isNull(),
                candidateMember.user.id.ne(userId),
                candidateMember.leftAt.isNull(),
                candidateUser.deletedAt.isNull(),
                candidateUser.role.ne(UserRole.ADMIN),
                containsUserKeyword(candidateUser, query)
            );
    }

    private JPAQuery<Long> createInvitableUsersCountQuery(
        Integer userId,
        String query,
        QChatRoomMember requesterMember,
        QChatRoomMember candidateMember,
        QUser candidateUser
    ) {
        // offset/limit가 적용된 본문 쿼리만으로는 전체 개수를 알 수 없어 count 쿼리를 분리한다.
        return jpaQueryFactory.select(candidateUser.id.countDistinct())
            .from(requesterMember)
            .join(candidateMember)
            .on(candidateMember.chatRoom.id.eq(requesterMember.chatRoom.id))
            .join(candidateMember.user, candidateUser)
            .where(
                requesterMember.user.id.eq(userId),
                requesterMember.leftAt.isNull(),
                candidateMember.user.id.ne(userId),
                candidateMember.leftAt.isNull(),
                candidateUser.deletedAt.isNull(),
                candidateUser.role.ne(UserRole.ADMIN),
                containsUserKeyword(candidateUser, query)
            );
    }

    public List<ClubMember> findRequesterClubMemberships(Integer userId) {
        QClubMember requesterClubMember = new QClubMember("requesterClubMember");

        // 서비스가 섹션 이름과 순서를 바로 쓸 수 있게 요청자 동아리를 fetch join으로 한 번에 읽는다.
        return jpaQueryFactory.select(requesterClubMember)
            .from(requesterClubMember)
            .join(requesterClubMember.club, club).fetchJoin()
            .join(requesterClubMember.user).fetchJoin()
            .where(requesterClubMember.user.id.eq(userId))
            .orderBy(club.name.asc(), club.id.asc())
            .fetch();
    }

    public List<ClubMember> findSharedClubMemberships(Integer userId, List<Integer> candidateUserIds) {
        if (candidateUserIds.isEmpty()) {
            return List.of();
        }

        QClubMember requesterClubMember = new QClubMember("requesterClubMember");
        QClubMember candidateClubMember = new QClubMember("candidateClubMember");
        QUser candidateUser = new QUser("candidateUser");

        // 대표 섹션 후보는 요청자와 실제로 공유하는 동아리만 남겨야 하므로 club_member를 다시 조인한다.
        return jpaQueryFactory.select(candidateClubMember)
            .from(candidateClubMember)
            .join(candidateClubMember.club, club).fetchJoin()
            .join(candidateClubMember.user, candidateUser).fetchJoin()
            .join(requesterClubMember)
            .on(
                requesterClubMember.club.id.eq(candidateClubMember.club.id)
                    .and(requesterClubMember.user.id.eq(userId))
            )
            .where(
                // 현재 페이지 후보 집합 안에서만 대표 동아리를 고르면 서비스 단계의 중복 배치를 막을 수 있다.
                candidateClubMember.user.id.in(candidateUserIds),
                candidateUser.deletedAt.isNull()
            )
            // putIfAbsent로 첫 동아리를 대표값으로 고를 수 있도록 동아리명/이름순으로 정렬한다.
            .orderBy(
                club.name.asc(),
                candidateUser.name.asc(),
                candidateUser.studentNumber.asc(),
                candidateUser.id.asc()
            )
            .fetch();
    }

    private BooleanExpression containsUserKeyword(QUser candidateUser, String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }

        String normalizedQuery = query.trim().toLowerCase();
        return candidateUser.name.lower().contains(normalizedQuery)
            .or(candidateUser.studentNumber.contains(normalizedQuery));
    }
}
