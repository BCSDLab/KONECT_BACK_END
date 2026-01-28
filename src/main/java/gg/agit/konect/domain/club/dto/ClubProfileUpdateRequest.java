package gg.agit.konect.domain.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClubProfileUpdateRequest(
    @Schema(description = "동아리 한 줄 소개", example = "즐겁게 일하고 열심히 노는 IT 특성화 동아리",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "한 줄 소개는 필수 입력입니다.")
    @Size(max = 20, message = "한 줄 소개는 20자 이하여야 합니다.")
    String introduce,

    @Schema(description = "동아리 로고 이미지 URL", example = "https://example.com/logo.png",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이미지 URL은 필수 입력입니다.")
    @Size(max = 255, message = "이미지 URL은 255자 이하여야 합니다.")
    String imageUrl
) {
}
