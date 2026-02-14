package gg.agit.konect.domain.chat.direct.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.chat.direct.model.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChatRoomResponse(
    @Schema(description = "채팅방 ID", example = "1", requiredMode = REQUIRED)
    Integer chatRoomId
) {
    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(chatRoom.getId());
    }
}
