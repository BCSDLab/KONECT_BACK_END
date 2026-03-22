package gg.agit.konect.domain.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ClubSheetIdUpdateRequest(
    @NotBlank(message = "스프레드시트 URL은 필수 입력입니다.")
    @Pattern(
        regexp = "^https://docs\\.google\\.com/spreadsheets/.*",
        message = "유효한 구글 스프레드시트 URL을 입력해주세요."
    )
    @Schema(
        description = "등록할 구글 스프레드시트 URL",
        example = "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms/edit"
    )
    String spreadsheetUrl
) {
}
