package gg.agit.konect.domain.notification.enums;

public enum NotificationInboxType {
    CLUB_APPLICATION_SUBMITTED,
    CLUB_APPLICATION_APPROVED,
    CLUB_APPLICATION_REJECTED,
    CHAT_MESSAGE,
    GROUP_CHAT_MESSAGE,
    UNREAD_CHAT_COUNT;

    public boolean isChatRelated() {
        return this == CHAT_MESSAGE
            || this == GROUP_CHAT_MESSAGE
            || this == UNREAD_CHAT_COUNT;
    }
}
