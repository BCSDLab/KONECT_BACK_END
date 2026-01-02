package gg.agit.konect.domain.studytime.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record StudyTimeSummaryResponse(
    @Schema(description = "오늘 누적 공부 시간(누적 초)", example = "45296", requiredMode = REQUIRED)
    Long todayStudyTime,

    @Schema(description = "총 누적 공부 시간(누적 초)", example = "564325", requiredMode = REQUIRED)
    Long totalStudyTime
) {
    public static StudyTimeSummaryResponse of(Long todayStudyTime, Long totalStudyTime) {
        return new StudyTimeSummaryResponse(todayStudyTime, totalStudyTime);
    }
}
