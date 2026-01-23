package gg.agit.konect.admin.schedule.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record AdminScheduleUpsertRequest(
    @NotEmpty(message = "일정 목록은 필수 입력입니다.")
    @Schema(description = "생성/수정할 일정 목록", requiredMode = REQUIRED)
    List<@Valid AdminScheduleUpsertItemRequest> schedules
) {
}
