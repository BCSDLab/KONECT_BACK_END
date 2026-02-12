package gg.agit.konect.domain.groupchat.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.groupchat.model.GroupChatMessage;

public interface GroupChatMessageRepository extends Repository<GroupChatMessage, Integer> {

    GroupChatMessage save(GroupChatMessage message);

    Optional<GroupChatMessage> findById(Integer messageId);

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

    long countByRoomIdAndCreatedAtGreaterThanEqual(Integer roomId, LocalDateTime joinedAt);

    @Query("""
        SELECT m
        FROM GroupChatMessage m
        WHERE m.room.id = :roomId
        AND m.createdAt >= :joinedAt
        AND NOT EXISTS (
            SELECT 1
            FROM MessageReadStatus rs
            WHERE rs.messageId = m.id
            AND rs.userId = :userId
        )
        """)
    List<GroupChatMessage> findUnreadMessagesByRoomIdAndUserIdAndCreatedAtGreaterThanEqual(
        @Param("roomId") Integer roomId,
        @Param("userId") Integer userId,
        @Param("joinedAt") LocalDateTime joinedAt
    );

    Optional<GroupChatMessage> findTopByRoomIdOrderByCreatedAtDesc(Integer roomId);
}
