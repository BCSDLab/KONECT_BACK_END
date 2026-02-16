package gg.agit.konect.domain.chat.direct.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChatRoomCreateRequest(
    @NotNull(message = "유저 ID는 필수입니다.")
    @Schema(description = "채팅 대상 유저 ID", example = "10", requiredMode = REQUIRED)
    Integer userId
) {

}
