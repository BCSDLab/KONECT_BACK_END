package gg.agit.konect.domain.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.notification.dto.NotificationInboxesResponse;
import gg.agit.konect.domain.notification.dto.NotificationInboxUnreadCountResponse;
import gg.agit.konect.domain.notification.service.NotificationInboxService;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications/inbox")
public class NotificationInboxController implements NotificationInboxApi {

    private final NotificationInboxService notificationInboxService;

    @Override
    public ResponseEntity<NotificationInboxesResponse> getMyInboxes(Integer userId, int page) {
        return ResponseEntity.ok(notificationInboxService.getMyInboxes(userId, page));
    }

    @Override
    public ResponseEntity<NotificationInboxUnreadCountResponse> getUnreadCount(Integer userId) {
        return ResponseEntity.ok(notificationInboxService.getUnreadCount(userId));
    }

    @Override
    public ResponseEntity<Void> markAsRead(Integer userId, Integer notificationId) {
        notificationInboxService.markAsRead(userId, notificationId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> markAllAsRead(Integer userId) {
        notificationInboxService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
}
