package gg.agit.konect.domain.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClubUpdateRequest(
    @Schema(description = "동아리 한 줄 소개", example = "즐겁게 일하고 열심히 노는 IT 특성화 동아리",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "한 줄 소개는 필수 입력입니다.")
    @Size(max = 20, message = "한 줄 소개는 20자 이하여야 합니다.")
    String description,

    @Schema(description = "동아리 로고 이미지 URL", example = "https://example.com/logo.png",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이미지 URL은 필수 입력입니다.")
    @Size(max = 255, message = "이미지 URL은 255자 이하여야 합니다.")
    String imageUrl,

    @Schema(description = "동아리 방 위치", example = "학생회관 101호",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "동아리 위치는 필수 입력입니다.")
    @Size(max = 255, message = "동아리 위치는 255자 이하여야 합니다.")
    String location,

    @Schema(description = "동아리 상세 소개", example = "BCSD에서 얻을 수 있는 경험\n1. IT 실무 경험",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "상세 소개는 필수 입력입니다.")
    String introduce
) {
}
