package gg.agit.konect.domain.chat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;

public interface ChatMessageRepository extends Repository<ChatMessage, Integer> {

    @Query("""
        SELECT cm
        FROM ChatMessage cm
        WHERE cm.chatRoom = :chatRoom
        ORDER BY cm.createdAt DESC
        LIMIT 1
        """)
    Optional<ChatMessage> findLastMessageByChatRoom(@Param("chatRoom") ChatRoom chatRoom);

    @Query("""
        SELECT COUNT(cm)
        FROM ChatMessage cm
        WHERE cm.chatRoom = :chatRoom
        AND cm.receiver.id = :receiverId
        AND cm.isRead = false
        """)
    Integer countUnreadMessages(@Param("chatRoom") ChatRoom chatRoom, @Param("receiverId") Integer receiverId);
}
