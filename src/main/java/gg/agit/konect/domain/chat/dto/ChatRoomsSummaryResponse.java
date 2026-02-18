package gg.agit.konect.domain.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChatRoomsSummaryResponse(
    @Schema(description = "채팅방 리스트", requiredMode = REQUIRED)
    List<ChatRoomSummaryResponse> rooms
) {
}
