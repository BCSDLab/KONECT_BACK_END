package gg.agit.konect.domain.chat.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.chat.dto.UnreadMessageCount;
import gg.agit.konect.domain.chat.model.ChatMessage;

public interface ChatMessageRepository extends Repository<ChatMessage, Integer> {

    ChatMessage save(ChatMessage chatMessage);

    @Query("""
        SELECT new gg.agit.konect.domain.chat.dto.UnreadMessageCount(
            cm.chatRoom.id,
            COUNT(cm)
        )
        FROM ChatMessage cm
        WHERE cm.chatRoom.id IN :chatRoomIds
        AND cm.receiver.id = :receiverId
        AND cm.isRead = false
        GROUP BY cm.chatRoom.id
        """)
    List<UnreadMessageCount> countUnreadMessagesByChatRoomIdsAndUserId(
        @Param("chatRoomIds") List<Integer> chatRoomIds,
        @Param("receiverId") Integer receiverId
    );

    @Query("""
        SELECT cm
        FROM ChatMessage cm
        JOIN FETCH cm.sender
        WHERE cm.chatRoom.id = :chatRoomId
        ORDER BY cm.createdAt DESC
        """)
    Page<ChatMessage> findByChatRoomId(@Param("chatRoomId") Integer chatRoomId, Pageable pageable);

    @Query("""
        SELECT cm
        FROM ChatMessage cm
        WHERE cm.chatRoom.id = :chatRoomId
        AND cm.receiver.id = :receiverId
        AND cm.isRead = false
        """)
    List<ChatMessage> findUnreadMessagesByChatRoomIdAndUserId(
        @Param("chatRoomId") Integer chatRoomId,
        @Param("receiverId") Integer receiverId
    );

    @Modifying
    @Query("""
        DELETE FROM ChatMessage cm
        WHERE cm.sender.id = :userId OR cm.receiver.id = :userId
        """)
    void deleteByUserId(@Param("userId") Integer userId);
}
