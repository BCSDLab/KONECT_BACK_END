package gg.agit.konect.domain.notification.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

public record FcmTokenRegisterRequest(
    @NotEmpty(message = "FCM 토큰은 필수 입력입니다.")
    @Schema(description = "FCM 디바이스 토큰", example = "fcm_token_value", requiredMode = REQUIRED)
    String token
) {
}
