package gg.agit.konect.domain.notification.service;

import static gg.agit.konect.global.code.ApiResponseCode.FAILED_SEND_FCM;
import static gg.agit.konect.global.code.ApiResponseCode.INVALID_FCM_TOKEN;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;

import gg.agit.konect.domain.notification.dto.FcmNotificationSendRequest;
import gg.agit.konect.domain.notification.dto.FcmTokenDeleteRequest;
import gg.agit.konect.domain.notification.dto.FcmTokenRegisterRequest;
import gg.agit.konect.domain.notification.model.FcmDeviceToken;
import gg.agit.konect.domain.notification.repository.FcmDeviceTokenRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final UserRepository userRepository;
    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;

    @Transactional
    public void registerToken(Integer userId, FcmTokenRegisterRequest request) {
        User user = userRepository.getById(userId);
        if (request.token().isBlank()) {
            throw CustomException.of(INVALID_FCM_TOKEN);
        }

        fcmDeviceTokenRepository.findByToken(request.token())
            .ifPresentOrElse(
                token -> token.updateUser(user),
                () -> fcmDeviceTokenRepository.save(FcmDeviceToken.of(user, request.token()))
            );
    }

    @Transactional
    public void deleteToken(Integer userId, FcmTokenDeleteRequest request) {
        fcmDeviceTokenRepository.findByUserIdAndToken(userId, request.token())
            .ifPresent(fcmDeviceTokenRepository::delete);
    }

    public void sendToMe(Integer userId, FcmNotificationSendRequest request) {
        List<String> tokens = fcmDeviceTokenRepository.findByUserId(userId).stream()
            .map(FcmDeviceToken::getToken)
            .toList();

        if (tokens.isEmpty()) {
            return;
        }

        MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
            .addAllTokens(tokens)
            .setNotification(
                Notification.builder()
                    .setTitle(request.title())
                    .setBody(request.body())
                    .build()
            );

        Map<String, String> data = request.data();
        if (data != null && !data.isEmpty()) {
            messageBuilder.putAllData(data);
        }

        try {
            FirebaseMessaging.getInstance().sendEachForMulticast(messageBuilder.build());
        } catch (FirebaseMessagingException exception) {
            throw CustomException.of(FAILED_SEND_FCM);
        }
    }
}
