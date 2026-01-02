package gg.agit.konect.domain.studytime.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StudyTimerStopRequest(
    @NotNull(message = "누적 초(totalSeconds)는 필수 입력입니다.")
    @Min(value = 0, message = "누적 초는 0 이상이어야 합니다.")
    @Schema(description = "타이머 누적 시간(초)", example = "5415", requiredMode = REQUIRED)
    Long totalSeconds
) {
}
