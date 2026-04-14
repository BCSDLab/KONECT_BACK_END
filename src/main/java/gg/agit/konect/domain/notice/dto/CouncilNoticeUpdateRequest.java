package gg.agit.konect.domain.notice.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CouncilNoticeUpdateRequest(
    @NotBlank(message = "공지사항 제목은 필수 입력입니다.")
    @Size(max = 255, message = "공지사항 제목은 최대 255자 입니다.")
    @Schema(description = "공지사항 제목", example = "동아리 박람회 참가 신청 마감 안내", requiredMode = REQUIRED)
    String title,

    @NotBlank(message = "공지사항 내용은 필수 입력입니다.")
    @Schema(description = "공지사항 내용", example = "2025년 동아리 박람회 참가 신청이 12월 15일까지입니다.", requiredMode = REQUIRED)
    String content
) {
}
