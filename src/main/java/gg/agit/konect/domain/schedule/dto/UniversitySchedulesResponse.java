package gg.agit.konect.domain.schedule.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

public record UniversitySchedulesResponse(
    @Schema(description = "대학교 일정 리스트", requiredMode = REQUIRED)
    List<InnerUniversityScheduleResponse> universitySchedules
) {
    public record InnerUniversityScheduleResponse(
        @Schema(description = "대학교 일정 제목", example = "동아리 박람회", requiredMode = REQUIRED)
        String title,

        @Schema(description = "대학교 일정 날짜", example = "2024.12.24", requiredMode = REQUIRED)
        @JsonFormat(pattern = "yyyy.MM.dd")
        LocalDate startedAt,

        @Schema(description = "대학교 일정 시각", example = "10:00 ~ 16:00", requiredMode = NOT_REQUIRED)
        String time,

        @Schema(description = "대학교 일정 디데이", example = "5", requiredMode = REQUIRED)
        Integer dDay
    ) {

    }
}
