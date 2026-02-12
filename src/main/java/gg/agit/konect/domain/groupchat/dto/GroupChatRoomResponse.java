package gg.agit.konect.domain.groupchat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record GroupChatRoomResponse(
    @Schema(description = "단체 채팅방 ID", example = "1", requiredMode = REQUIRED)
    Integer groupChatRoomId
) {
}
