package gg.agit.konect.domain.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageSendRequest(
    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Size(max = 1000, message = "1000자 이내로 입력해주세요.")
    @Schema(description = "메시지 내용", example = "투명 케이스가 끼워져 있었어요!", requiredMode = REQUIRED)
    String content
) {

}
