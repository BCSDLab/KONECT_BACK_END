package gg.agit.konect.domain.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.notification.model.NotificationDeviceToken;

public interface NotificationDeviceTokenRepository extends Repository<NotificationDeviceToken, Integer> {

    Optional<NotificationDeviceToken> findByToken(String token);

    Optional<NotificationDeviceToken> findByUserIdAndDeviceId(Integer userId, String deviceId);

    List<NotificationDeviceToken> findByUserId(Integer userId);

    void save(NotificationDeviceToken notificationDeviceToken);

    void delete(NotificationDeviceToken notificationDeviceToken);
}
