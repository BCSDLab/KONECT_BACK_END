package gg.agit.konect.domain.notification.dto;

import java.time.LocalDateTime;

import gg.agit.konect.domain.notification.enums.NotificationInboxType;
import gg.agit.konect.domain.notification.model.NotificationInbox;

public record NotificationInboxResponse(
    Integer id,
    NotificationInboxType type,
    String title,
    String body,
    String path,
    Boolean isRead,
    LocalDateTime createdAt
) {
    public static NotificationInboxResponse from(NotificationInbox inbox) {
        return new NotificationInboxResponse(
            inbox.getId(),
            inbox.getType(),
            inbox.getTitle(),
            inbox.getBody(),
            inbox.getPath(),
            inbox.getIsRead(),
            inbox.getCreatedAt()
        );
    }
}
