package gg.agit.konect.domain.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SheetMigrateRequest(
    @NotBlank
    @Schema(
        description = "동아리가 기존에 사용하던 구글 스프레드시트 URL",
        example = "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5.../edit"
    )
    String sourceSpreadsheetUrl
) {
}
