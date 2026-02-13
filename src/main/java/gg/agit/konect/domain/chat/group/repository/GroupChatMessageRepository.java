package gg.agit.konect.domain.chat.group.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.chat.group.model.GroupChatMessage;

public interface GroupChatMessageRepository extends Repository<GroupChatMessage, Integer> {

    GroupChatMessage save(GroupChatMessage message);

    Optional<GroupChatMessage> findById(Integer messageId);

    @Query("""
        SELECT m
        FROM GroupChatMessage m
        WHERE m.room.id = :roomId
        AND m.id > :minMessageId
        ORDER BY m.createdAt DESC
        """)
    Page<GroupChatMessage> findByRoomIdAndIdGreaterThanOrderByCreatedAtDesc(
        Integer roomId,
        Integer minMessageId,
        Pageable pageable
    );

    @Query("""
        SELECT m
        FROM GroupChatMessage m
        JOIN FETCH m.sender
        WHERE m.room.id = :roomId
        AND m.createdAt >= :joinedAt
        ORDER BY m.createdAt DESC
        """)
    Page<GroupChatMessage> findByRoomIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
        @Param("roomId") Integer roomId,
        @Param("joinedAt") LocalDateTime joinedAt,
        Pageable pageable
    );

    @Query("""
        SELECT COUNT(m)
        FROM GroupChatMessage m
        WHERE m.room.id = :roomId
        AND m.createdAt >= :joinedAt
        """)
    long countByRoomIdAndCreatedAtGreaterThanEqual(
        @Param("roomId") Integer roomId,
        @Param("joinedAt") LocalDateTime joinedAt
    );

    @Query("""
        SELECT COUNT(m)
        FROM GroupChatMessage m
        WHERE m.room.id = :roomId
        AND m.createdAt > :lastReadAt
        AND m.sender.id <> :senderId
        """)
    long countByRoomIdAndCreatedAtGreaterThanAndSenderIdNot(
        @Param("roomId") Integer roomId,
        @Param("lastReadAt") LocalDateTime lastReadAt,
        @Param("senderId") Integer senderId
    );

    @Query("""
        SELECT m
        FROM GroupChatMessage m
        JOIN FETCH m.sender
        WHERE m.id IN (
            SELECT MAX(m2.id)
            FROM GroupChatMessage m2
            WHERE m2.room.id IN :roomIds
            GROUP BY m2.room.id
        )
        """)
    List<GroupChatMessage> findLatestMessagesByRoomIds(@Param("roomIds") List<Integer> roomIds);

    @Query(value = """
        SELECT r.id AS roomId, COUNT(m.id) AS unreadCount
        FROM group_chat_room r
        JOIN club_member cm
            ON cm.club_id = r.club_id
            AND cm.user_id = :userId
        LEFT JOIN group_chat_read_status grs
            ON grs.room_id = r.id
            AND grs.user_id = :userId
        LEFT JOIN group_chat_message m
            ON m.room_id = r.id
            AND m.sender_id <> :userId
            AND m.created_at > COALESCE(grs.last_read_at, cm.created_at)
        WHERE r.id IN (:roomIds)
        GROUP BY r.id
        """, nativeQuery = true)
    List<GroupRoomUnreadCountProjection> countUnreadByRoomIdsAndUserId(
        @Param("roomIds") List<Integer> roomIds,
        @Param("userId") Integer userId
    );

    @Query("""
        SELECT m
        FROM GroupChatMessage m
        WHERE m.room.id = :roomId
        ORDER BY m.createdAt DESC
        """)
    Optional<GroupChatMessage> findTopByRoomIdOrderByCreatedAtDesc(@Param("roomId") Integer roomId);
}
