package gg.agit.konect.domain.schedule.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import gg.agit.konect.domain.schedule.model.UniversitySchedule;
import io.swagger.v3.oas.annotations.media.Schema;

public record UniversitySchedulesResponse(
    @Schema(description = "대학교 일정 리스트", requiredMode = REQUIRED)
    List<InnerUniversityScheduleResponse> universitySchedules
) {
    public record InnerUniversityScheduleResponse(
        @Schema(description = "대학교 일정 제목", example = "동아리 박람회", requiredMode = REQUIRED)
        String title,

        @Schema(description = "대학교 일정 시작일", example = "2025.12.03", requiredMode = REQUIRED)
        @JsonFormat(pattern = "yyyy.MM.dd")
        LocalDate startedDate,

        @Schema(description = "대학교 일정 시작시간", example = "10:00", requiredMode = NOT_REQUIRED)
        @JsonFormat(pattern = "HH:mm")
        LocalTime startedTime,

        @Schema(description = "대학교 일정 종료일", example = "2025.12.05", requiredMode = REQUIRED)
        @JsonFormat(pattern = "yyyy.MM.dd")
        LocalDate endedDate,

        @Schema(description = "대학교 일정 종료시간", example = "18:00", requiredMode = NOT_REQUIRED)
        @JsonFormat(pattern = "HH:mm")
        LocalTime endedTime,

        @Schema(description = "대학교 일정 디데이", example = "5", requiredMode = NOT_REQUIRED)
        Integer dDay
    ) {
        public static InnerUniversityScheduleResponse from(UniversitySchedule schedule) {
            LocalDate today = LocalDate.now();

            return new InnerUniversityScheduleResponse(
                schedule.getTitle(),
                schedule.getStartedDate(),
                schedule.getStartedTime(),
                schedule.getEndedDate(),
                schedule.getEndedTime(),
                schedule.calculateDDay(today)
            );
        }
    }
    public static UniversitySchedulesResponse from(List<UniversitySchedule> universitySchedules) {
        return new UniversitySchedulesResponse(
            universitySchedules.stream()
                .map(InnerUniversityScheduleResponse::from)
                .toList()
        );
    }
}
