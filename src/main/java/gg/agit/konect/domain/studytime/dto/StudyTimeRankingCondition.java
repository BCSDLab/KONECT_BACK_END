package gg.agit.konect.domain.studytime.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.studytime.enums.StudyTimeRankingSort;
import gg.agit.konect.domain.studytime.enums.StudyTimeRankingType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StudyTimeRankingCondition(
    @Schema(description = "페이지 번호", example = "1", defaultValue = "1")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    Integer page,

    @Schema(description = "페이지 당 항목 수", example = "20", defaultValue = "20")
    @Min(value = 1, message = "페이지 당 항목 수는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 당 항목 수는 100 이하여야 합니다.")
    Integer limit,

    @Schema(description = "랭킹 기준", example = "CLUB", requiredMode = REQUIRED)
    @NotNull(message = "랭킹 기준은 필수입니다.")
    StudyTimeRankingType type,

    @Schema(description = "정렬 기준", example = "MONTHLY", defaultValue = "MONTHLY")
    StudyTimeRankingSort sort
) {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final StudyTimeRankingSort DEFAULT_SORT = StudyTimeRankingSort.MONTHLY;

    public StudyTimeRankingCondition {
        page = page != null ? page : DEFAULT_PAGE;
        limit = limit != null ? limit : DEFAULT_LIMIT;
        sort = sort != null ? sort : DEFAULT_SORT;
    }
}
