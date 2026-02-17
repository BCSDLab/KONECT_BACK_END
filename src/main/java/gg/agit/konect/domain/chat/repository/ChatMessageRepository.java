package gg.agit.konect.domain.chat.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.chat.dto.UnreadMessageCount;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.user.enums.UserRole;

public interface ChatMessageRepository extends Repository<ChatMessage, Integer> {

    ChatMessage save(ChatMessage chatMessage);

    @Query("""
        SELECT new gg.agit.konect.domain.chat.dto.UnreadMessageCount(
            crm.id.chatRoomId,
            COUNT(cm)
        )
        FROM ChatRoomMember crm
        LEFT JOIN ChatMessage cm ON cm.chatRoom.id = crm.id.chatRoomId
            AND cm.sender.id <> :receiverId
            AND cm.createdAt > crm.lastReadAt
        WHERE crm.id.chatRoomId IN :chatRoomIds
          AND crm.id.userId = :receiverId
        GROUP BY crm.id.chatRoomId
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
        SELECT new gg.agit.konect.domain.chat.dto.UnreadMessageCount(
            crm.id.chatRoomId,
            COUNT(cm)
        )
        FROM ChatRoomMember crm
        JOIN crm.user u
        LEFT JOIN ChatMessage cm ON cm.chatRoom.id = crm.id.chatRoomId
            AND cm.sender.role != :adminRole
            AND cm.createdAt > crm.lastReadAt
        WHERE crm.id.chatRoomId IN :chatRoomIds
          AND u.role = :adminRole
        GROUP BY crm.id.chatRoomId
        """)
    List<UnreadMessageCount> countUnreadMessagesForAdmin(
        @Param("chatRoomIds") List<Integer> chatRoomIds,
        @Param("adminRole") UserRole adminRole
    );

    @Query("""
        SELECT m
        FROM ChatMessage m
        JOIN FETCH m.sender
        WHERE m.id IN (
            SELECT MAX(m2.id)
            FROM ChatMessage m2
            WHERE m2.chatRoom.id IN :roomIds
            GROUP BY m2.chatRoom.id
        )
        """)
    List<ChatMessage> findLatestMessagesByRoomIds(@Param("roomIds") List<Integer> roomIds);

    @Query("""
        SELECT COUNT(m)
        FROM ChatMessage m
        WHERE m.chatRoom.id = :chatRoomId
        """)
    long countByChatRoomId(@Param("chatRoomId") Integer chatRoomId);
}
