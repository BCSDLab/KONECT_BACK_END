package gg.agit.konect.admin.schedule.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import gg.agit.konect.domain.schedule.model.ScheduleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminScheduleUpsertItemRequest(
    @Schema(description = "수정할 일정 ID (없으면 신규 생성)", example = "1")
    Integer scheduleId,

    @NotBlank
    @Schema(description = "일정 제목", example = "동계방학", requiredMode = REQUIRED)
    String title,

    @NotNull
    @Schema(description = "일정 시작 일시", example = "2025.12.22 00:00:00", requiredMode = REQUIRED)
    @JsonFormat(pattern = "yyyy.MM.dd HH:mm:ss")
    LocalDateTime startedAt,

    @NotNull
    @Schema(description = "일정 종료 일시", example = "2026.02.27 23:59:59", requiredMode = REQUIRED)
    @JsonFormat(pattern = "yyyy.MM.dd HH:mm:ss")
    LocalDateTime endedAt,

    @NotNull
    @Schema(description = "일정 종류", example = "UNIVERSITY", requiredMode = REQUIRED)
    ScheduleType scheduleType
) {
}
