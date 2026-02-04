package gg.agit.konect.domain.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.notification.dto.NotificationSendRequest;
import gg.agit.konect.domain.notification.dto.NotificationTokenDeleteRequest;
import gg.agit.konect.domain.notification.dto.NotificationTokenRegisterRequest;
import gg.agit.konect.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController implements NotificationApi {

    private final NotificationService notificationService;

    @Override
    public ResponseEntity<Void> registerToken(Integer userId, NotificationTokenRegisterRequest request) {
        notificationService.registerToken(userId, request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteToken(Integer userId, NotificationTokenDeleteRequest request) {
        notificationService.deleteToken(userId, request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> sendToMe(Integer userId, NotificationSendRequest request) {
        notificationService.sendToMe(userId, request);
        return ResponseEntity.ok().build();
    }
}
