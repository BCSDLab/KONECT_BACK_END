package gg.agit.konect.domain.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gg.agit.konect.domain.notification.dto.NotificationInboxesResponse;
import gg.agit.konect.domain.notification.dto.NotificationInboxUnreadCountResponse;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;

@Validated
@Tag(name = "(Normal) Notification: 알림", description = "알림 API")
@RequestMapping("/notifications/inbox")
public interface NotificationInboxApi {

    @Operation(summary = "인앱 알림 목록을 조회한다.")
    @GetMapping
    ResponseEntity<NotificationInboxesResponse> getMyInboxes(
        @UserId Integer userId,
        @RequestParam(defaultValue = "1") @Min(1) int page
    );

    @Operation(summary = "인앱 알림 미읽음 개수를 조회한다.")
    @GetMapping("/unread-count")
    ResponseEntity<NotificationInboxUnreadCountResponse> getUnreadCount(
        @UserId Integer userId
    );

    @Operation(summary = "인앱 알림을 읽음 처리한다.")
    @PatchMapping("/{notificationId}/read")
    ResponseEntity<Void> markAsRead(
        @UserId Integer userId,
        @PathVariable Integer notificationId
    );

    @Operation(summary = "인앱 알림 전체를 읽음 처리한다.")
    @PatchMapping("/read-all")
    ResponseEntity<Void> markAllAsRead(
        @UserId Integer userId
    );
}
