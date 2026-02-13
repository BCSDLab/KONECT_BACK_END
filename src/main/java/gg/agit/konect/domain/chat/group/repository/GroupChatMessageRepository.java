package gg.agit.konect.domain.chat.group.repository;

import java.time.LocalDateTime;
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

    long countByRoomIdAndCreatedAtGreaterThanAndSenderIdNot(Integer roomId, LocalDateTime lastReadAt, Integer senderId);

    Optional<GroupChatMessage> findTopByRoomIdOrderByCreatedAtDesc(Integer roomId);
}
