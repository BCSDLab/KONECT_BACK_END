package gg.agit.konect.domain.studytime.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record StudyTimeSummaryResponse(
    @Schema(description = "오늘 누적 공부 시간(HH:mm:ss)", example = "03:30:15", requiredMode = REQUIRED)
    String todayStudyTime,

    @Schema(description = "총 누적 공부 시간(H+:mm:ss)", example = "120:10:05", requiredMode = REQUIRED)
    String totalStudyTime
) {
    public static StudyTimeSummaryResponse of(String todayStudyTime, String totalStudyTime) {
        return new StudyTimeSummaryResponse(todayStudyTime, totalStudyTime);
    }
}
