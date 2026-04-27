package gg.agit.konect.domain.chat.repository;

import static gg.agit.konect.domain.chat.model.QChatMessage.chatMessage;
import static gg.agit.konect.domain.chat.model.QChatRoom.chatRoom;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.QChatMessage;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ChatMessageQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<ChatMessage> searchLatestMatchingMessagesByChatRoomIds(
        List<Integer> roomIds,
        String keyword
    ) {
        if (roomIds.isEmpty()) {
            return List.of();
        }

        QChatMessage innerMessage = new QChatMessage("innerMessage");

        return jpaQueryFactory
            .selectFrom(chatMessage)
            .join(chatMessage.chatRoom, chatRoom).fetchJoin()
            .where(
                chatRoom.id.in(roomIds),
                containsKeyword(chatMessage, keyword),
                chatMessage.id.eq(
                    JPAExpressions
                        .select(innerMessage.id.max())
                        .from(innerMessage)
                        .where(
                            innerMessage.chatRoom.id.eq(chatRoom.id),
                            containsKeyword(innerMessage, keyword)
                        )
                )
            )
            .orderBy(chatMessage.createdAt.desc(), chatMessage.id.desc())
            .fetch();
    }

    private BooleanExpression containsKeyword(QChatMessage message, String keyword) {
        return Expressions.booleanTemplate(
            "LOCATE({0}, LOWER({1})) > 0",
            keyword.toLowerCase(Locale.ROOT),
            message.content
        );
    }
}
