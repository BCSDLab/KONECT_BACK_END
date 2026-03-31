package gg.agit.konect.domain.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record ChatRoomNameUpdateRequest(
    @Size(max = 30, message = "채팅방 이름은 30자 이내로 입력해주세요.")
    @Schema(
        description = "개인별 채팅방 이름. null 또는 공백이면 기본 이름으로 되돌립니다.",
        example = "알바 이야기방",
        requiredMode = NOT_REQUIRED
    )
    String roomName
) {
}
