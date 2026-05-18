package gg.agit.konect.domain.chat.dto;

import java.time.LocalDateTime;

/**
 * 관리자용 1:1 채팅방 목록 조회를 위한 Projection DTO
 * 필드 순서와 타입이 조회 쿼리의 constructor projection과 정확히 일치해야 합니다.
 */
public record AdminChatRoomProjection(
    Integer roomId,
    String lastMessage,
    LocalDateTime lastSentAt,
    LocalDateTime createdAt,
    Integer nonAdminUserId,
    String nonAdminUserName,
    String nonAdminImageUrl,
    Long unreadCount
) {
}
