package gg.agit.konect.domain.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClubDetailUpdateRequest(
    @Schema(description = "동아리 방 위치", example = "학생회관 101호", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "동아리 위치는 필수 입력입니다.")
    @Size(max = 255, message = "동아리 위치는 255자 이하여야 합니다.")
    String location,

    @Schema(description = "동아리 상세 소개", example = "BCSD에서 얻을 수 있는 경험\n1. IT 실무 경험",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "상세 소개는 필수 입력입니다.")
    String introduce
) {
}
