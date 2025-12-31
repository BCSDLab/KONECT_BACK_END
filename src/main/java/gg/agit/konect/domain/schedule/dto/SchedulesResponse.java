package gg.agit.konect.domain.schedule.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import gg.agit.konect.domain.schedule.model.Schedule;
import io.swagger.v3.oas.annotations.media.Schema;

public record SchedulesResponse(
    @Schema(description = "일정 리스트", requiredMode = REQUIRED)
    List<InnerScheduleResponse> schedules
) {
    public record InnerScheduleResponse(
        @Schema(description = "일정 제목", example = "동아리 박람회", requiredMode = REQUIRED)
        String title,

        @Schema(description = "일정 시작일시", example = "2025.12.03 00:00", requiredMode = REQUIRED)
        @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
        LocalDateTime startedAt,

        @Schema(description = "일정 종료일시", example = "2025.12.05 00:00", requiredMode = REQUIRED)
        @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
        LocalDateTime endedAt,

        @Schema(description = "일정 디데이", example = "5", requiredMode = NOT_REQUIRED)
        Integer dDay,

        @Schema(description = "일정 카테고리", example = "UNIVERSITY", requiredMode = REQUIRED)
        String scheduleCategory
    ) {
        public static InnerScheduleResponse from(Schedule schedule) {
            LocalDate today = LocalDate.now();

            return new InnerScheduleResponse(
                schedule.getTitle(),
                schedule.getStartedAt(),
                schedule.getEndedAt(),
                schedule.calculateDDay(today),
                schedule.getScheduleType()
            );
        }
    }

    public static SchedulesResponse from(List<Schedule> schedules) {
        return new SchedulesResponse(
            schedules.stream()
                .map(InnerScheduleResponse::from)
                .toList()
        );
    }
}
