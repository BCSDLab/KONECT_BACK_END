package gg.agit.konect.domain.chat.repository;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CHAT_ROOM;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.chat.dto.AdminChatRoomProjection;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.global.exception.CustomException;

public interface ChatRoomRepository extends Repository<ChatRoom, Integer> {

    ChatRoom save(ChatRoom chatRoom);

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

    /**
     * 관리자용 1:1 채팅방 목록을 Projection DTO로 최적화 조회
     * <p>
     * 사용자가 응답한 채팅방만 필터링하고, 필요한 필드만 한 번에 조회합니다.
     * 이 메소드는 다음과 같은 최적화를 제공합니다:
     * <ul>
     *   <li>ChatRoom 엔티티 전체 로딩 대신 필요한 필드만 Projection</li>
     *   <li>읽지 않은 메시지 수를 DB에서 직접 계산 (COUNT 서브쿼리)</li>
     *   <li>상대방 사용자 정보를 JOIN으로 한 번에 조회</li>
     * </ul>
     */
    @Query("""
        SELECT new gg.agit.konect.domain.chat.dto.AdminChatRoomProjection(
            cr.id,
            cr.lastMessageContent,
            cr.lastMessageSentAt,
            cr.createdAt,
            u.id,
            u.name,
            u.imageUrl,
            COUNT(cm)
        )
        FROM ChatRoom cr
        JOIN ChatRoomMember crm ON crm.id.chatRoomId = cr.id
        JOIN User u ON u.id = crm.id.userId
        JOIN ChatRoomMember adminCrm ON adminCrm.id.chatRoomId = cr.id
            AND adminCrm.id.userId = :systemAdminId
            AND adminCrm.leftAt IS NULL
        LEFT JOIN ChatMessage cm ON cm.chatRoom.id = cr.id
            AND cm.sender.id <> :systemAdminId
            AND cm.createdAt > adminCrm.lastReadAt
        WHERE cr.roomType = :roomType
          AND u.role != :adminRole
          AND EXISTS (
              SELECT 1 FROM ChatMessage userReply
              JOIN userReply.sender userSender
              WHERE userReply.chatRoom.id = cr.id
                AND userSender.role != :adminRole
          )
        GROUP BY cr.id, cr.lastMessageContent, cr.lastMessageSentAt, cr.createdAt, u.id, u.name, u.imageUrl
        ORDER BY COALESCE(cr.lastMessageSentAt, cr.createdAt) DESC
        """)
    List<AdminChatRoomProjection> findAdminChatRoomsOptimized(
        @Param("systemAdminId") Integer systemAdminId,
        @Param("adminRole") UserRole adminRole,
        @Param("roomType") ChatType roomType
    );
}
