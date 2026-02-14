package gg.agit.konect.domain.chat.direct.dto;

public record UnreadMessageCount(
    Integer chatRoomId,
    Long unreadCount
) {

}
