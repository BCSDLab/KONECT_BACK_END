package gg.agit.konect.domain.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CreateChatRoomRequest(
    @NotNull(message = "동아리 ID는 필수입니다.")
    @Schema(description = "동아리 ID", example = "1", requiredMode = REQUIRED)
    Integer clubId
) {

}