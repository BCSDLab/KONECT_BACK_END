package gg.agit.konect.domain.studytime.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public record StudyTimerStopRequest(
    @NotEmpty(message = "타이머 누적 시간은 필수 입력입니다.")
    @Pattern(regexp = "^[0-9]{2}:[0-5][0-9]:[0-5][0-9]$", message = "타이머 누적 시간 형식이 올바르지 않습니다.")
    @Schema(description = "타이머 누적 시간(HH:mm:ss)", example = "01:30:15", requiredMode = REQUIRED)
    String elapsedTime
) {
}
