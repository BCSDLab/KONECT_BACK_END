package gg.agit.konect.domain.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.chat.model.ChatRoom;

public interface ChatRoomRepository extends Repository<ChatRoom, Integer> {

    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        JOIN FETCH cr.sender
        JOIN FETCH cr.receiver
        LEFT JOIN FETCH cr.chatMessages
        WHERE cr.sender.id = :userId OR cr.receiver.id = :userId
        ORDER BY cr.updatedAt DESC
        """)
    List<ChatRoom> findByUserId(@Param("userId") Integer userId);

    @Query("""
        SELECT cr
        FROM ChatRoom cr
        JOIN FETCH cr.sender
        JOIN FETCH cr.receiver
        WHERE cr.id = :chatRoomId
        """)
    ChatRoom getById(@Param("chatRoomId") Integer chatRoomId);
}
