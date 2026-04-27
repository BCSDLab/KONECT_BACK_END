package gg.agit.konect.domain.chat.repository;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CHAT_ROOM;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.global.exception.CustomException;

public interface ChatRoomRepository extends Repository<ChatRoom, Integer> {

    ChatRoom save(ChatRoom chatRoom);

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE ChatRoom cr
        SET cr.lastMessageContent = :content,
            cr.lastMessageSentAt = :sentAt
        WHERE cr.id = :roomId
          AND NOT EXISTS (
              SELECT 1
              FROM ChatMessage cm
              WHERE cm.chatRoom.id = :roomId
                AND cm.id <> :messageId
                AND (
                    cm.createdAt > :sentAt
                    OR (cm.createdAt = :sentAt AND cm.id > :messageId)
                )
          )
        """)
    int updateLastMessageIfLatest(
        @Param("roomId") Integer roomId,
        @Param("messageId") Integer messageId,
        @Param("content") String content,
        @Param("sentAt") LocalDateTime sentAt
    );

    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        JOIN ChatRoomMember crm ON crm.id.chatRoomId = cr.id
        LEFT JOIN FETCH cr.club
        WHERE crm.id.userId = :userId
          AND cr.roomType = :roomType
        ORDER BY cr.lastMessageSentAt DESC NULLS LAST, cr.id
        """)
    List<ChatRoom> findByUserId(@Param("userId") Integer userId, @Param("roomType") ChatType roomType);

    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        JOIN ChatRoomMember crm ON crm.id.chatRoomId = cr.id
        WHERE crm.id.userId = :userId
          AND cr.roomType = gg.agit.konect.domain.chat.enums.ChatType.GROUP
          AND crm.leftAt IS NULL
        ORDER BY COALESCE(cr.lastMessageSentAt, cr.createdAt) DESC
        """)
    List<ChatRoom> findGroupRoomsByMemberUserId(@Param("userId") Integer userId);

    @Query("""
        SELECT cr
        FROM ChatRoom cr
        LEFT JOIN FETCH cr.club
        WHERE cr.id = :chatRoomId
        """)
    Optional<ChatRoom> findById(@Param("chatRoomId") Integer chatRoomId);

    default ChatRoom getById(Integer chatRoomId) {
        return findById(chatRoomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));
    }

    @Query("""
        SELECT cr
        FROM ChatRoom cr
        JOIN ChatRoomMember crm
            ON crm.id.chatRoomId = cr.id
        WHERE cr.roomType = :roomType
        GROUP BY cr
        HAVING COUNT(crm) = 2
           AND SUM(CASE WHEN crm.id.userId = :userId1 THEN 1 ELSE 0 END) = 1
           AND SUM(CASE WHEN crm.id.userId = :userId2 THEN 1 ELSE 0 END) = 1
        """)
    Optional<ChatRoom> findByTwoUsers(
        @Param("userId1") Integer userId1,
        @Param("userId2") Integer userId2,
        @Param("roomType") ChatType roomType
    );

    @Query("""
        SELECT cr
        FROM ChatRoom cr
        LEFT JOIN FETCH cr.club c
        WHERE c.id = :clubId
        """)
    Optional<ChatRoom> findByClubId(@Param("clubId") Integer clubId);

    @Query("""
        SELECT cr
        FROM ChatRoom cr
        LEFT JOIN FETCH cr.club c
        WHERE c.id IN :clubIds
        """)
    List<ChatRoom> findByClubIds(@Param("clubIds") List<Integer> clubIds);

    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        JOIN ChatRoomMember crm ON crm.id.chatRoomId = cr.id
        LEFT JOIN FETCH cr.club
        WHERE crm.id.userId = :userId
          AND cr.roomType = :roomType
          AND cr.club IS NOT NULL
        ORDER BY cr.lastMessageSentAt DESC NULLS LAST, cr.id
        """)
    List<ChatRoom> findGroupRoomsByUserId(@Param("userId") Integer userId, @Param("roomType") ChatType roomType);

    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        WHERE cr.roomType = :roomType
          AND EXISTS (
              SELECT 1 FROM ChatRoomMember adminMember
              JOIN adminMember.user adminUser
              WHERE adminMember.id.chatRoomId = cr.id
                AND adminUser.role = :adminRole
          )
          AND EXISTS (
              SELECT 1 FROM ChatRoomMember userMember
              JOIN userMember.user normalUser
              WHERE userMember.id.chatRoomId = cr.id
                AND normalUser.role != :adminRole
          )
        ORDER BY cr.lastMessageSentAt DESC NULLS LAST, cr.id
        """)
    List<ChatRoom> findAllAdminUserDirectRooms(
        @Param("adminRole") UserRole adminRole,
        @Param("roomType") ChatType roomType
    );

    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        WHERE cr.roomType = :roomType
          AND EXISTS (
              SELECT 1 FROM ChatRoomMember systemAdminMember
              WHERE systemAdminMember.id.chatRoomId = cr.id
                AND systemAdminMember.id.userId = :systemAdminId
          )
          AND EXISTS (
              SELECT 1 FROM ChatRoomMember userMember
              JOIN userMember.user normalUser
              WHERE userMember.id.chatRoomId = cr.id
                AND normalUser.role != :adminRole
          )
        ORDER BY cr.lastMessageSentAt DESC NULLS LAST, cr.id
        """)
    List<ChatRoom> findAllSystemAdminDirectRooms(
        @Param("systemAdminId") Integer systemAdminId,
        @Param("adminRole") UserRole adminRole,
        @Param("roomType") ChatType roomType
    );

}
