package gg.agit.konect.domain.chat.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    // ORDER BY는 countNewerMessagesByChatRoomId의 WHERE 조건과
    // 일치해야 함. 페이지 계산 정확도가 두 쿼리의 정렬 일관성에 의존함.
    @Query("""
        SELECT cm
        FROM ChatMessage cm
        JOIN FETCH cm.sender
        WHERE cm.chatRoom.id = :chatRoomId
          AND (:visibleMessageFrom IS NULL OR cm.createdAt > :visibleMessageFrom)
        ORDER BY cm.createdAt DESC, cm.id DESC
        """)
    Page<ChatMessage> findByChatRoomId(
        @Param("chatRoomId") Integer chatRoomId,
        @Param("visibleMessageFrom") LocalDateTime visibleMessageFrom,
        Pageable pageable
    );

    @Query("""
        SELECT COUNT(m)
        FROM ChatMessage m
        WHERE m.chatRoom.id = :chatRoomId
          AND (:visibleMessageFrom IS NULL OR m.createdAt > :visibleMessageFrom)
        """)
    long countByChatRoomId(
        @Param("chatRoomId") Integer chatRoomId,
        @Param("visibleMessageFrom") LocalDateTime visibleMessageFrom
    );

    @Query("""
        SELECT new gg.agit.konect.domain.chat.dto.UnreadMessageCount(
            cr.id,
            (SELECT COUNT(cm)
             FROM ChatMessage cm
             WHERE cm.chatRoom.id = cr.id
               AND cm.sender.role != :adminRole
               AND cm.createdAt > (
                   SELECT COALESCE(MAX(crm.lastReadAt), cr.createdAt)
                   FROM ChatRoomMember crm
                   JOIN crm.user u
                   WHERE crm.id.chatRoomId = cr.id
                     AND u.role = :adminRole
               )
            )
        )
        FROM ChatRoom cr
        WHERE cr.id IN :chatRoomIds
        """)
    List<UnreadMessageCount> countUnreadMessagesForAdmin(
        @Param("chatRoomIds") List<Integer> chatRoomIds,
        @Param("adminRole") UserRole adminRole
    );

    @Query("""
        SELECT COUNT(cm) > 0
        FROM ChatMessage cm
        WHERE cm.chatRoom.id = :chatRoomId
          AND cm.sender.role != :adminRole
        """)
    boolean existsUserReplyByRoomId(
        @Param("chatRoomId") Integer chatRoomId,
        @Param("adminRole") UserRole adminRole
    );

    @Query("""
        SELECT DISTINCT cm.chatRoom.id
        FROM ChatMessage cm
        WHERE cm.chatRoom.id IN :chatRoomIds
          AND cm.sender.role != :adminRole
        """)
    List<Integer> findRoomIdsWithUserReplyByRoomIds(
        @Param("chatRoomIds") List<Integer> chatRoomIds,
        @Param("adminRole") UserRole adminRole
    );

    @Query("SELECT cm FROM ChatMessage cm JOIN FETCH cm.chatRoom WHERE cm.id = :messageId")
    Optional<ChatMessage> findByIdWithChatRoom(@Param("messageId") Integer messageId);

    // ORDER BY 기준이 findByChatRoomId와 일치해야 함 (createdAt DESC, id DESC).
    // 페이지 계산 정확도가 두 쿼리의 정렬 일관성에 의존함.
    @Query("""
        SELECT COUNT(m)
        FROM ChatMessage m
        WHERE m.chatRoom.id = :chatRoomId
          AND (m.createdAt > :createdAt OR (m.createdAt = :createdAt AND m.id > :messageId))
          AND (:visibleMessageFrom IS NULL OR m.createdAt > :visibleMessageFrom)
        """)
    long countNewerMessagesByChatRoomId(
        @Param("chatRoomId") Integer chatRoomId,
        @Param("messageId") Integer messageId,
        @Param("createdAt") LocalDateTime createdAt,
        @Param("visibleMessageFrom") LocalDateTime visibleMessageFrom
    );

    @Query("""
        SELECT COUNT(m)
        FROM ChatMessage m
        WHERE m.chatRoom.id = :chatRoomId
        """)
    long countByChatRoomId(@Param("chatRoomId") Integer chatRoomId);
}
