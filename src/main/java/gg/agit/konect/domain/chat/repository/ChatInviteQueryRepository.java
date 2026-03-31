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
        // 같은 chat_room_member 테이블을 두 번 조인하는 self join 이라 별도 alias가 필요하다.
        QChatRoomMember requesterMember = new QChatRoomMember("requesterMember");
        QChatRoomMember candidateMember = new QChatRoomMember("candidateMember");
        QUser candidateUser = new QUser("candidateUser");

        // content 쿼리는 현재 페이지에 실제로 보여줄 초대 후보만 가져온다.
        List<User> content = jpaQueryFactory.select(candidateUser)
            .distinct()
            .from(requesterMember)
            .join(candidateMember)
            .on(candidateMember.chatRoom.id.eq(requesterMember.chatRoom.id))
            .join(candidateMember.user, candidateUser)
            .where(
                // 내가 아직 나가지 않은 채팅방만 초대 후보 탐색의 시작점이 된다.
                requesterMember.user.id.eq(userId),
                requesterMember.leftAt.isNull(),
                // 자기 자신은 초대 대상이 될 수 없고, 상대도 현재 방에 남아 있어야 한다.
                candidateMember.user.id.ne(userId),
                candidateMember.leftAt.isNull(),
                // 탈퇴 유저와 관리자 계정은 신규 채팅방 초대 대상에서 제외한다.
                candidateUser.deletedAt.isNull(),
                candidateUser.role.ne(UserRole.ADMIN),
                // 검색어는 이름/학번 부분 일치로만 적용한다.
                containsUserKeyword(candidateUser, query)
            )
            // 페이지가 달라도 사용자 노출 순서는 항상 같아야 하므로 DB 정렬을 고정한다.
            .orderBy(
                candidateUser.name.asc(),
                candidateUser.studentNumber.asc(),
                candidateUser.id.asc()
            )
            .offset(pageRequest.getOffset())
            .limit(pageRequest.getPageSize())
            .fetch();

        // offset/limit가 적용된 본문 쿼리만으로는 전체 개수를 알 수 없어 count 쿼리를 분리한다.
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
