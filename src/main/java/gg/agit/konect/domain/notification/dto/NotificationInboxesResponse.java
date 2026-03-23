package gg.agit.konect.domain.notification.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import gg.agit.konect.domain.notification.model.NotificationInbox;

public record NotificationInboxesResponse(
    List<NotificationInboxResponse> notifications,
    int currentPage,
    int totalPages,
    long totalElements,
    boolean hasNext
) {
    public static NotificationInboxesResponse from(Page<NotificationInbox> page) {
        return new NotificationInboxesResponse(
            page.getContent().stream().map(NotificationInboxResponse::from).toList(),
            page.getNumber() + 1,
            page.getTotalPages(),
            page.getTotalElements(),
            page.hasNext()
        );
    }
}
