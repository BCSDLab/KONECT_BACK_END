package gg.agit.konect.domain.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ClubCondition(
    @Schema(description = "페이지 번호", example = "1", defaultValue = "1")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    Integer page,

    @Schema(description = "페이지 당 항목 수", example = "10", defaultValue = "10")
    @Min(value = 1, message = "페이지 당 항목 수는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 당 항목 수는 100 이하여야 합니다.")
    Integer limit,

    @Schema(description = "검색 키워드", example = "Bcsd", defaultValue = "")
    String query,

    @Schema(description = "모집 중인 동아리 조회 여부", example = "false", defaultValue = "false")
    Boolean isRecruiting
) {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 10;
    private static final String DEFAULT_QUERY = "";
    private static final boolean DEFAULT_IS_RECRUITING = false;

    public ClubCondition {
        page = page != null ? page : DEFAULT_PAGE;
        limit = limit != null ? limit : DEFAULT_LIMIT;
        query = query != null ? query : DEFAULT_QUERY;
        isRecruiting = isRecruiting != null ? isRecruiting : DEFAULT_IS_RECRUITING;
    }
}
