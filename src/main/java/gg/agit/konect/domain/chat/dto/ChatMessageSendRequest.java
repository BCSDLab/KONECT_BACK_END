package gg.agit.konect.domain.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ChatMessageSendRequest(
    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Schema(description = "메시지 내용", example = "투명 케이스가 끼워져 있었어요!", requiredMode = REQUIRED)
    String content
) {

}
