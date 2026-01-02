package gg.agit.konect.domain.studytime.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.studytime.model.StudyTimeSummary;
import io.swagger.v3.oas.annotations.media.Schema;

public record StudyTimerStopResponse(
    @Schema(description = "이번 세션 공부 시간(초)", example = "3600", requiredMode = REQUIRED)
    Long sessionSeconds,

    @Schema(description = "오늘 누적 공부 시간(초)", example = "7200", requiredMode = REQUIRED)
    Long dailySeconds,

    @Schema(description = "이번 달 누적 공부 시간(초)", example = "120000", requiredMode = REQUIRED)
    Long monthlySeconds,

    @Schema(description = "총 누적 공부 시간(초)", example = "360000", requiredMode = REQUIRED)
    Long totalSeconds
) {
    public static StudyTimerStopResponse from(StudyTimeSummary summary) {
        return new StudyTimerStopResponse(
            summary.sessionSeconds(),
            summary.dailySeconds(),
            summary.monthlySeconds(),
            summary.totalSeconds()
        );
    }
}
