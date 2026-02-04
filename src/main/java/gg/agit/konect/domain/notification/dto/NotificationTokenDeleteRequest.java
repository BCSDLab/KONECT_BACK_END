package gg.agit.konect.domain.notification.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

public record NotificationTokenDeleteRequest(
    @NotEmpty(message = "디바이스 식별자는 필수 입력입니다.")
    @Schema(description = "디바이스 식별자", example = "device-uuid", requiredMode = REQUIRED)
    String deviceId
) {
}
