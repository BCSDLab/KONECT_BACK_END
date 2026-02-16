package gg.agit.konect.domain.chat.unified.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record UnifiedChatRoomsResponse(
    @Schema(description = "채팅방 리스트", requiredMode = REQUIRED)
    List<UnifiedChatRoomResponse> rooms
) {
}
