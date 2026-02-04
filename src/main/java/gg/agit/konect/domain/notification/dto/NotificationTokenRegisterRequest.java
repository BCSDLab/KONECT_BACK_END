package gg.agit.konect.domain.notification.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.version.enums.PlatformType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record NotificationTokenRegisterRequest(
    @NotEmpty(message = "푸시 토큰은 필수 입력입니다.")
    @Schema(description = "푸시 디바이스 토큰", example = "ExpoPushToken[xxxx]", requiredMode = REQUIRED)
    String token,

    @NotEmpty(message = "디바이스 식별자는 필수 입력입니다.")
    @Schema(description = "디바이스 식별자", example = "device-uuid", requiredMode = REQUIRED)
    String deviceId,

    @NotNull(message = "플랫폼은 필수 입력입니다.")
    @Schema(description = "플랫폼", example = "IOS", requiredMode = REQUIRED)
    PlatformType platform
) {
}
