package gg.agit.konect.domain.chat.group.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record GroupChatMuteResponse(
    @Schema(description = "단체 채팅 알림 음소거 여부", example = "true", requiredMode = REQUIRED)
    Boolean isMuted
) {
}
