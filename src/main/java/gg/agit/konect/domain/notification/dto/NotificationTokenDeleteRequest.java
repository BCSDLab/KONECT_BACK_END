package gg.agit.konect.domain.notification.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

public record NotificationTokenDeleteRequest(
    @NotEmpty(message = "알림 토큰은 필수 입력입니다.")
    @Schema(description = "알림 디바이스 토큰", example = "ExpoPushToken[xxxx]", requiredMode = REQUIRED)
    String token
) {
}
