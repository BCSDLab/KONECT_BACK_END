package gg.agit.konect.domain.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.notification.model.NotificationDeviceToken;

public interface NotificationDeviceTokenRepository extends Repository<NotificationDeviceToken, Integer> {

    Optional<NotificationDeviceToken> findByUserId(Integer userId);

    @Query("""
        SELECT ndt
        FROM NotificationDeviceToken ndt
        WHERE ndt.user.id = :userId
        AND ndt.token = :token
        """)
    Optional<NotificationDeviceToken> findByUserIdAndToken(
        @Param("userId") Integer userId,
        @Param("token") String token
    );

    @Query("""
        SELECT ndt.token
        FROM NotificationDeviceToken ndt
        WHERE ndt.user.id = :userId
        """)
    List<String> findTokensByUserId(@Param("userId") Integer userId);

    void save(NotificationDeviceToken notificationDeviceToken);

    void delete(NotificationDeviceToken notificationDeviceToken);
}
