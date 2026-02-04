package gg.agit.konect.domain.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.notification.model.FcmDeviceToken;

public interface FcmDeviceTokenRepository extends Repository<FcmDeviceToken, Integer> {

    Optional<FcmDeviceToken> findByToken(String token);

    Optional<FcmDeviceToken> findByUserIdAndToken(Integer userId, String token);

    List<FcmDeviceToken> findByUserId(Integer userId);

    void save(FcmDeviceToken fcmDeviceToken);

    void delete(FcmDeviceToken fcmDeviceToken);
}
