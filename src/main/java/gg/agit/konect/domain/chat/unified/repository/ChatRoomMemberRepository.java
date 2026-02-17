package gg.agit.konect.domain.chat.unified.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.chat.unified.model.ChatRoomMember;
import gg.agit.konect.domain.chat.unified.model.ChatRoomMemberId;

public interface ChatRoomMemberRepository extends Repository<ChatRoomMember, ChatRoomMemberId> {

    ChatRoomMember save(ChatRoomMember chatRoomMember);

    @Query("""
        SELECT crm
        FROM ChatRoomMember crm
        WHERE crm.id.chatRoomId = :chatRoomId
          AND crm.id.userId = :userId
        """)
    Optional<ChatRoomMember> findByChatRoomIdAndUserId(
        @Param("chatRoomId") Integer chatRoomId,
        @Param("userId") Integer userId
    );

    @Query("""
        SELECT COUNT(crm) > 0
        FROM ChatRoomMember crm
        WHERE crm.id.chatRoomId = :chatRoomId
          AND crm.id.userId = :userId
        """)
    boolean existsByChatRoomIdAndUserId(
        @Param("chatRoomId") Integer chatRoomId,
        @Param("userId") Integer userId
    );

    @Query("""
        SELECT crm
        FROM ChatRoomMember crm
        WHERE crm.id.chatRoomId = :chatRoomId
        """)
    List<ChatRoomMember> findByChatRoomId(@Param("chatRoomId") Integer chatRoomId);

    @Query("""
        SELECT crm
        FROM ChatRoomMember crm
        WHERE crm.id.userId = :userId
        """)
    List<ChatRoomMember> findByUserId(@Param("userId") Integer userId);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ChatRoomMember crm
        SET crm.lastReadAt = :lastReadAt
        WHERE crm.id.chatRoomId = :chatRoomId
          AND crm.id.userId = :userId
          AND crm.lastReadAt < :lastReadAt
        """)
    int updateLastReadAtIfOlder(
        @Param("chatRoomId") Integer chatRoomId,
        @Param("userId") Integer userId,
        @Param("lastReadAt") LocalDateTime lastReadAt
    );

    @Query(value = """
        SELECT crm.chat_room_id AS roomId, COUNT(cm.id) AS unreadCount
        FROM chat_room_member crm
                 LEFT JOIN chat_message cm
                           ON cm.chat_room_id = crm.chat_room_id
                               AND cm.sender_id <> :userId
                               AND cm.created_at > crm.last_read_at
        WHERE crm.user_id = :userId
          AND crm.chat_room_id IN (:roomIds)
        GROUP BY crm.chat_room_id
        """, nativeQuery = true)
    List<RoomUnreadCountProjection> countUnreadByRoomIdsAndUserId(
        @Param("roomIds") List<Integer> roomIds,
        @Param("userId") Integer userId
    );
}
