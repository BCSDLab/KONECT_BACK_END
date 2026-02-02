package gg.agit.konect.domain.club.dto;

import org.springframework.data.domain.Sort;

import gg.agit.konect.domain.club.enums.ClubApplicationSortBy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ClubApplicationCondition(
    @Schema(description = "페이지 번호", example = "1", defaultValue = "1")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
    Integer page,

    @Schema(description = "페이지 당 항목 수", example = "10", defaultValue = "10")
    @Min(value = 1, message = "페이지 당 항목 수는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 당 항목 수는 100 이하여야 합니다")
    Integer limit,

    @Schema(description = "정렬 기준", example = "APPLIED_AT", defaultValue = "APPLIED_AT")
    ClubApplicationSortBy sortBy,

    @Schema(description = "정렬 방향", example = "DESC", defaultValue = "DESC")
    Sort.Direction sortDirection
) {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 10;
    private static final ClubApplicationSortBy DEFAULT_SORT_BY = ClubApplicationSortBy.APPLIED_AT;
    private static final Sort.Direction DEFAULT_SORT_DIRECTION = Sort.Direction.DESC;

    public ClubApplicationCondition {
        page = page != null ? page : DEFAULT_PAGE;
        limit = limit != null ? limit : DEFAULT_LIMIT;
        sortBy = sortBy != null ? sortBy : DEFAULT_SORT_BY;
        sortDirection = sortDirection != null ? sortDirection : DEFAULT_SORT_DIRECTION;
    }
}
