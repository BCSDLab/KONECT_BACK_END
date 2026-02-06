package gg.agit.konect.domain.inquiry.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record InquiryRequest(
    @NotBlank(message = "문의 내용은 필수 입력입니다.")
    @Schema(description = "어드민 문의 내용", example = "앱 사용 중 오류가 발생했습니다.", requiredMode = REQUIRED)
    String content
) {
}
