package gg.agit.konect.domain.notification.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import gg.agit.konect.domain.notification.service.NotificationInboxSseService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications/inbox")
public class NotificationInboxSseController implements NotificationInboxSseApi {

    private final NotificationInboxSseService notificationInboxSseService;

    @Override
    public SseEmitter subscribe(Integer userId) {
        return notificationInboxSseService.subscribe(userId);
    }
}
