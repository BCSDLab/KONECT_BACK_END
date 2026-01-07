package gg.agit.konect.domain.studytime.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.studytime.enums.StudyTimeRankingSort;
import io.swagger.v3.oas.annotations.media.Schema;

public record StudyTimeMyRankingCondition(
    @Schema(description = "순위 기준", example = "MONTHLY", defaultValue = "MONTHLY", requiredMode = REQUIRED)
    StudyTimeRankingSort sort
) {
    private static final StudyTimeRankingSort DEFAULT_SORT = StudyTimeRankingSort.MONTHLY;

    public StudyTimeMyRankingCondition {
        sort = sort != null ? sort : DEFAULT_SORT;
    }
}
