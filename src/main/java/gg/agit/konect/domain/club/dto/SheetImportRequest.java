package gg.agit.konect.domain.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SheetImportRequest(
    @NotBlank
    @Pattern(
        regexp = "^[A-Za-z0-9_-]+$",
        message = "스프레드시트 ID는 영문자, 숫자, 하이픈(-), 언더스코어(_)만 허용합니다."
    )
    @Schema(
        description = "인명부가 담긴 구글 스프레드시트 ID",
        example = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms"
    )
    String spreadsheetId
) {
}
