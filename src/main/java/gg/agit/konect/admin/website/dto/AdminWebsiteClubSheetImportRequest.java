package gg.agit.konect.admin.website.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminWebsiteClubSheetImportRequest(

    @Schema(
        description = "동아리 등록 양식 Google Sheets URL",
        example = "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms/edit"
    )
    @NotBlank
    @Pattern(regexp = "^https://docs\\.google\\.com/spreadsheets/(?:u/\\d+/)?d/[A-Za-z0-9_-]+.*")
    String spreadsheetUrl
) {
}
