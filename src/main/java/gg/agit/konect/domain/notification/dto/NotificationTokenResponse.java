package gg.agit.konect.domain.notification.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.notification.model.NotificationDeviceToken;
import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationTokenResponse(
    @Schema(description = "알림 디바이스 토큰", example = "ExpoPushToken[xxxx]", requiredMode = REQUIRED)
    String token
) {

    public static NotificationTokenResponse from(NotificationDeviceToken notificationDeviceToken) {
        return new NotificationTokenResponse(notificationDeviceToken.getToken());
    }
}
