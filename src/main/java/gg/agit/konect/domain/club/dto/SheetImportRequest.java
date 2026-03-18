package gg.agit.konect.domain.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SheetImportRequest(
    @NotBlank
    @Schema(
        description = "인명부가 담긴 구글 스프레드시트 ID 또는 URL",
        example = "1BxiMVs0XRA5..."
    )
    String spreadsheetId
) {
}
