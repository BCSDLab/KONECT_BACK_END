package gg.agit.konect.domain.chat.repository;

import static gg.agit.konect.domain.club.model.QClub.club;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import gg.agit.konect.domain.chat.model.QChatRoomMember;
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

        List<User> content = jpaQueryFactory.select(candidateUser)
            .distinct()
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
            )
            .orderBy(
                candidateUser.name.asc(),
                candidateUser.studentNumber.asc(),
                candidateUser.id.asc()
            )
            .offset(pageRequest.getOffset())
            .limit(pageRequest.getPageSize())
            .fetch();

        Long total = jpaQueryFactory.select(candidateUser.id.countDistinct())
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
            )
            .fetchOne();

        return new PageImpl<>(content, pageRequest, total == null ? 0 : total);
    }

    public List<ClubMember> findRequesterClubMemberships(Integer userId) {
        QClubMember requesterClubMember = new QClubMember("requesterClubMember");

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
                candidateClubMember.user.id.in(candidateUserIds),
                candidateUser.deletedAt.isNull()
            )
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
