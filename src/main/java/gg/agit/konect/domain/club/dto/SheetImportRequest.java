package gg.agit.konect.domain.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SheetImportRequest(
    @NotBlank
    @Pattern(
        regexp = "^https://docs\\.google\\.com/spreadsheets/(?:u/\\d+/)?d/[A-Za-z0-9_-]+.*",
        message = "유효한 구글 스프레드시트 URL을 입력해주세요."
    )
    @Schema(
        description = "인명부가 담긴 구글 스프레드시트 URL",
        example = "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms/edit"
    )
    String spreadsheetUrl
) {
}
