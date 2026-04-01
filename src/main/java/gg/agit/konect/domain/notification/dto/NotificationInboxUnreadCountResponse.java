package gg.agit.konect.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationInboxUnreadCountResponse(
    @Schema(description = "미읽은 알림 개수", example = "5")
    long unreadCount
) {
    public static NotificationInboxUnreadCountResponse of(long count) {
        return new NotificationInboxUnreadCountResponse(count);
    }
}
