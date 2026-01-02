package gg.agit.konect.domain.studytime.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record StudyTimeSummaryResponse(
    @Schema(description = "오늘 누적 공부 시간(누적 초)", example = "45296", requiredMode = REQUIRED)
    Long todayStudyTime,

    @Schema(description = "월간 누적 공부 시간(누적 초)", example = "334510", requiredMode = REQUIRED)
    Long monthlyStudyTime,

    @Schema(description = "총 누적 공부 시간(누적 초)", example = "564325", requiredMode = REQUIRED)
    Long totalStudyTime
) {
    public static StudyTimeSummaryResponse of(Long todayStudyTime, Long monthlyStudyTime, Long totalStudyTime) {
        return new StudyTimeSummaryResponse(todayStudyTime, monthlyStudyTime, totalStudyTime);
    }
}
