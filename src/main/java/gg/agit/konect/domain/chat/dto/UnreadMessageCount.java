package gg.agit.konect.domain.chat.dto;

public record UnreadMessageCount(
    Integer chatRoomId,
    Long unreadCount
) {

}
