package gg.agit.konect.domain.chat.repository;

import static gg.agit.konect.domain.chat.model.QChatMessage.chatMessage;
import static gg.agit.konect.domain.chat.model.QChatRoom.chatRoom;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import gg.agit.konect.domain.chat.dto.AdminChatRoomProjection;
import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.model.QChatMessage;
import gg.agit.konect.domain.chat.model.QChatRoomMember;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.QUser;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ChatRoomQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<AdminChatRoomProjection> findAdminChatRoomsOptimized(
        Integer systemAdminId,
        Integer viewerAdminId,
        UserRole adminRole,
        ChatType roomType
    ) {
        QChatRoomMember roomMember = new QChatRoomMember("roomMember");
        QChatRoomMember systemAdminMember = new QChatRoomMember("systemAdminMember");
        QChatRoomMember viewerAdminMember = new QChatRoomMember("viewerAdminMember");
        QUser nonAdminUser = new QUser("nonAdminUser");
        QChatMessage userReply = new QChatMessage("userReply");
        QUser userReplySender = new QUser("userReplySender");

        return jpaQueryFactory
            .select(Projections.constructor(
                AdminChatRoomProjection.class,
                chatRoom.id,
                chatRoom.lastMessageContent,
                chatRoom.lastMessageSentAt,
                chatRoom.createdAt,
                nonAdminUser.id,
                nonAdminUser.name,
                nonAdminUser.imageUrl,
                chatMessage.count()
            ))
            .from(chatRoom)
            .join(roomMember).on(roomMember.id.chatRoomId.eq(chatRoom.id))
            .join(roomMember.user, nonAdminUser)
            .join(systemAdminMember).on(
                systemAdminMember.id.chatRoomId.eq(chatRoom.id),
                systemAdminMember.id.userId.eq(systemAdminId)
            )
            .leftJoin(viewerAdminMember).on(
                viewerAdminMember.id.chatRoomId.eq(chatRoom.id),
                viewerAdminMember.id.userId.eq(viewerAdminId)
            )
            .leftJoin(chatMessage).on(
                chatMessage.chatRoom.id.eq(chatRoom.id),
                chatMessage.sender.id.ne(systemAdminId),
                chatMessage.createdAt.gt(systemAdminMember.lastReadAt)
            )
            .where(
                chatRoom.roomType.eq(roomType),
                nonAdminUser.role.ne(adminRole),
                nonAdminUser.deletedAt.isNull(),
                // 관리자는 문의방 멤버가 아니거나 나갔어도 새 메시지가 있으면 목록에서 다시 볼 수 있다.
                viewerAdminMember.leftAt.isNull()
                    .or(chatRoom.lastMessageSentAt.gt(viewerAdminMember.visibleMessageFrom)),
                JPAExpressions
                    .selectOne()
                    .from(userReply)
                    .join(userReply.sender, userReplySender)
                    .where(
                        userReply.chatRoom.id.eq(chatRoom.id),
                        userReplySender.role.ne(adminRole)
                    )
                    .exists()
            )
            .groupBy(
                chatRoom.id,
                chatRoom.lastMessageContent,
                chatRoom.lastMessageSentAt,
                chatRoom.createdAt,
                nonAdminUser.id,
                nonAdminUser.name,
                nonAdminUser.imageUrl
            )
            .orderBy(chatRoom.lastMessageSentAt.coalesce(chatRoom.createdAt).desc())
            .fetch();
    }
}
