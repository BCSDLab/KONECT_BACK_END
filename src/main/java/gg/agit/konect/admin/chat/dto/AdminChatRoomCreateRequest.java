package gg.agit.konect.admin.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AdminChatRoomCreateRequest(
    @NotNull(message = "유저 ID는 필수입니다.")
    @Schema(description = "대화할 유저 ID", example = "1", requiredMode = REQUIRED)
    Integer userId
) {

}
