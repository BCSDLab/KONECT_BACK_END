package gg.agit.konect.admin.schedule.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AdminScheduleUpsertRequest(
    @NotNull
    @NotEmpty
    @Valid
    @Schema(description = "생성/수정할 일정 목록", requiredMode = REQUIRED)
    List<AdminScheduleUpsertItemRequest> schedules
) {
}
