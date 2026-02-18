package gg.agit.konect.domain.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.user.enums.UserRole;

public interface ChatRoomRepository extends Repository<ChatRoom, Integer> {

    ChatRoom save(ChatRoom chatRoom);

    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        JOIN ChatRoomMember crm ON crm.id.chatRoomId = cr.id
        LEFT JOIN FETCH cr.club
        WHERE crm.id.userId = :userId
          AND cr.club IS NULL
        ORDER BY cr.lastMessageSentAt DESC NULLS LAST, cr.id
        """)
    List<ChatRoom> findByUserId(@Param("userId") Integer userId);

    @Query("""
        SELECT cr
        FROM ChatRoom cr
        LEFT JOIN FETCH cr.club
        WHERE cr.id = :chatRoomId
        """)
    Optional<ChatRoom> findById(@Param("chatRoomId") Integer chatRoomId);

    @Query("""
        SELECT cr
        FROM ChatRoom cr
        JOIN ChatRoomMember crm
            ON crm.id.chatRoomId = cr.id
        WHERE cr.club IS NULL
        GROUP BY cr
        HAVING COUNT(crm) = 2
           AND SUM(CASE WHEN crm.id.userId = :userId1 THEN 1 ELSE 0 END) = 1
           AND SUM(CASE WHEN crm.id.userId = :userId2 THEN 1 ELSE 0 END) = 1
        """)
    Optional<ChatRoom> findByTwoUsers(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);

    @Query("""
        SELECT cr
        FROM ChatRoom cr
        LEFT JOIN FETCH cr.club c
        WHERE c.id = :clubId
        """)
    Optional<ChatRoom> findByClubId(@Param("clubId") Integer clubId);

    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        JOIN ChatRoomMember crm ON crm.id.chatRoomId = cr.id
        LEFT JOIN FETCH cr.club
        WHERE crm.id.userId = :userId
          AND cr.club IS NOT NULL
        ORDER BY cr.lastMessageSentAt DESC NULLS LAST, cr.id
        """)
    List<ChatRoom> findGroupRoomsByUserId(@Param("userId") Integer userId);

    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        WHERE cr.club IS NULL
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
    List<ChatRoom> findAllAdminUserDirectRooms(@Param("adminRole") UserRole adminRole);
}
