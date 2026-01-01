package gg.agit.konect.domain.schedule.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScheduleCondition(
    @Schema(description = "조회할 년도", example = "2025", requiredMode = REQUIRED)
    @NotNull(message = "년도는 필수입니다.")
    @Min(value = 2000, message = "년도는 2000년 이상이어야 합니다.")
    @Max(value = 2100, message = "년도는 2100년 이하여야 합니다.")
    Integer year,

    @Schema(description = "조회할 월", example = "12", requiredMode = REQUIRED)
    @NotNull(message = "월은 필수입니다.")
    @Min(value = 1, message = "월은 1 이상이어야 합니다.")
    @Max(value = 12, message = "월은 12 이하여야 합니다.")
    Integer month
) {

}
