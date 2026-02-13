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

    Optional<ChatRoom> findById(Integer chatRoomId);

    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        JOIN FETCH cr.sender
        JOIN FETCH cr.receiver
        WHERE cr.sender.id = :userId OR cr.receiver.id = :userId
        ORDER BY cr.lastMessageSentAt DESC NULLS LAST, cr.id
        """)
    List<ChatRoom> findByUserId(@Param("userId") Integer userId);

    @Query("""
        SELECT cr
        FROM ChatRoom cr
        JOIN FETCH cr.sender
        JOIN FETCH cr.receiver
        WHERE cr.id = :chatRoomId
        """)
    Optional<ChatRoom> findById(@Param("chatRoomId") Integer chatRoomId);

    @Query("""
        SELECT cr
        FROM ChatRoom cr
        WHERE (cr.sender.id = :userId1 AND cr.receiver.id = :userId2)
           OR (cr.sender.id = :userId2 AND cr.receiver.id = :userId1)
        """)
    Optional<ChatRoom> findByTwoUsers(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);

    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        JOIN FETCH cr.sender s
        JOIN FETCH cr.receiver r
        WHERE s.role = :adminRole OR r.role = :adminRole
        ORDER BY cr.lastMessageSentAt DESC NULLS LAST, cr.id
        """)
    List<ChatRoom> findAllAdminChatRooms(@Param("adminRole") UserRole adminRole);

    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        JOIN FETCH cr.sender s
        JOIN FETCH cr.receiver r
        WHERE (s.role = :adminRole OR r.role = :adminRole)
        AND EXISTS (
            SELECT 1 FROM ChatMessage cm
            WHERE cm.chatRoom = cr
            AND cm.sender.role != :adminRole
        )
        ORDER BY cr.lastMessageSentAt DESC NULLS LAST, cr.id
        """)
    List<ChatRoom> findAdminChatRoomsWithUserReply(@Param("adminRole") UserRole adminRole);

    @Query("""
        SELECT cr
        FROM ChatRoom cr
        JOIN FETCH cr.sender s
        JOIN FETCH cr.receiver r
        WHERE (s.id = :userId AND r.role = :adminRole)
           OR (s.role = :adminRole AND r.id = :userId)
        """)
    Optional<ChatRoom> findByUserIdAndAdminRole(
        @Param("userId") Integer userId,
        @Param("adminRole") UserRole adminRole
    );
}
