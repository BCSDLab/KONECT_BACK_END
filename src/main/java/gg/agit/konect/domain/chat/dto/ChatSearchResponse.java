package gg.agit.konect.domain.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChatSearchResponse(
    @Schema(description = "채팅방 이름으로 매칭된 검색 결과", requiredMode = REQUIRED)
    ChatRoomMatchesResponse roomMatches,

    @Schema(description = "메시지 내용으로 매칭된 검색 결과", requiredMode = REQUIRED)
    ChatMessageMatchesResponse messageMatches
) {
}
