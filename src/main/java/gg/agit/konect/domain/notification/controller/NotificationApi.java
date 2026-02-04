package gg.agit.konect.domain.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.notification.dto.FcmNotificationSendRequest;
import gg.agit.konect.domain.notification.dto.FcmTokenDeleteRequest;
import gg.agit.konect.domain.notification.dto.FcmTokenRegisterRequest;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Notification: 푸시 알림", description = "푸시 알림 API")
@RequestMapping("/notifications")
public interface NotificationApi {

    @Operation(summary = "FCM 토큰을 등록한다.")
    @PostMapping("/tokens")
    ResponseEntity<Void> registerToken(
        @UserId Integer userId,
        @Valid @RequestBody FcmTokenRegisterRequest request
    );

    @Operation(summary = "FCM 토큰을 삭제한다.")
    @DeleteMapping("/tokens")
    ResponseEntity<Void> deleteToken(
        @UserId Integer userId,
        @Valid @RequestBody FcmTokenDeleteRequest request
    );

    @Operation(summary = "내 디바이스로 테스트 푸시를 전송한다.")
    @PostMapping("/send")
    ResponseEntity<Void> sendToMe(
        @UserId Integer userId,
        @Valid @RequestBody FcmNotificationSendRequest request
    );
}
