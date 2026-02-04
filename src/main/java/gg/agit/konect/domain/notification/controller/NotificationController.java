package gg.agit.konect.domain.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.notification.dto.FcmNotificationSendRequest;
import gg.agit.konect.domain.notification.dto.FcmTokenDeleteRequest;
import gg.agit.konect.domain.notification.dto.FcmTokenRegisterRequest;
import gg.agit.konect.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController implements NotificationApi {

    private final NotificationService notificationService;

    @Override
    public ResponseEntity<Void> registerToken(Integer userId, FcmTokenRegisterRequest request) {
        notificationService.registerToken(userId, request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteToken(Integer userId, FcmTokenDeleteRequest request) {
        notificationService.deleteToken(userId, request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> sendToMe(Integer userId, FcmNotificationSendRequest request) {
        notificationService.sendToMe(userId, request);
        return ResponseEntity.ok().build();
    }
}
