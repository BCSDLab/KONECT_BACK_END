package gg.agit.konect.domain.notification.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "(Normal) Notification: 알림", description = "알림 API")
@RequestMapping("/notifications/inbox")
public interface NotificationInboxSseApi {

    @Operation(summary = "인앱 알림 SSE 구독을 시작한다.")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter subscribe(@UserId Integer userId);
}
