package gg.agit.konect.domain.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.chat.model.ChatRoom;

public interface ChatRoomRepository extends Repository<ChatRoom, Integer> {

    ChatRoom save(ChatRoom chatRoom);

    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        JOIN FETCH cr.sender
        JOIN FETCH cr.receiver
        WHERE cr.sender.id = :userId OR cr.receiver.id = :userId
        ORDER BY cr.lastMessageSentAt DESC NULLS LAST, cr.id
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

    @Query("""
        SELECT cr
        FROM ChatRoom cr
        WHERE (cr.sender.id = :userId1 AND cr.receiver.id = :userId2)
           OR (cr.sender.id = :userId2 AND cr.receiver.id = :userId1)
        """)
    Optional<ChatRoom> findByTwoUsers(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);
}
