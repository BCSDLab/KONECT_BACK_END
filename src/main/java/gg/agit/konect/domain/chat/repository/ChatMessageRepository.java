package gg.agit.konect.domain.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.chat.model.ChatMessage;

public interface ChatMessageRepository extends Repository<ChatMessage, Integer> {

    @Query("""
        SELECT cm
        FROM ChatMessage cm
        JOIN FETCH cm.sender
        WHERE cm.chatRoom.id = :chatRoomId
        ORDER BY cm.createdAt ASC
        """)
    List<ChatMessage> findByChatRoomId(@Param("chatRoomId") Integer chatRoomId);
}
