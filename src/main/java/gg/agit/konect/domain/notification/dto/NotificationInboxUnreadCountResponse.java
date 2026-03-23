package gg.agit.konect.domain.notification.dto;

public record NotificationInboxUnreadCountResponse(long unreadCount) {
    public static NotificationInboxUnreadCountResponse of(long count) {
        return new NotificationInboxUnreadCountResponse(count);
    }
}
