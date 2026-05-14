package gg.agit.konect.domain.website.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.club.enums.ClubCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record WebsiteClubListCondition(
    @Schema(description = "페이지 번호", example = "1", requiredMode = REQUIRED)
    @Min(1)
    Integer page,

    @Schema(description = "페이지 크기", example = "12", requiredMode = REQUIRED)
    @Min(1)
    @Max(100)
    Integer limit,

    @Schema(description = "동아리명 검색어", example = "BCSD", requiredMode = NOT_REQUIRED)
    String query,

    @Schema(description = "동아리 분과", example = "ACADEMIC", requiredMode = NOT_REQUIRED)
    ClubCategory category
) {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 12;

    public WebsiteClubListCondition {
        page = page == null ? DEFAULT_PAGE : page;
        limit = limit == null ? DEFAULT_LIMIT : limit;
    }
}
