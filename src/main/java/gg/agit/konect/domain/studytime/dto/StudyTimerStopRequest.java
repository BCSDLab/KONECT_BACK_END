package gg.agit.konect.domain.studytime.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StudyTimerStopRequest(
    @NotNull(message = "시간(hour)은 필수 입력입니다.")
    @Min(value = 0, message = "시간은 0 이상이어야 합니다.")
    @Schema(description = "타이머 시간 - 시간", example = "1", requiredMode = REQUIRED)
    Integer hour,

    @NotNull(message = "분(minute)은 필수 입력입니다.")
    @Min(value = 0, message = "분은 0 이상이어야 합니다.")
    @Max(value = 59, message = "분은 59 이하여야 합니다.")
    @Schema(description = "타이머 시간 - 분", example = "30", requiredMode = REQUIRED)
    Integer minute,

    @NotNull(message = "초(second)는 필수 입력입니다.")
    @Min(value = 0, message = "초는 0 이상이어야 합니다.")
    @Max(value = 59, message = "초는 59 이하여야 합니다.")
    @Schema(description = "타이머 시간 - 초", example = "15", requiredMode = REQUIRED)
    Integer second
) {

    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long SECONDS_PER_HOUR = 3600L;

    public long toTotalSeconds() {
        return hour * SECONDS_PER_HOUR + minute * SECONDS_PER_MINUTE + second;
    }
}
