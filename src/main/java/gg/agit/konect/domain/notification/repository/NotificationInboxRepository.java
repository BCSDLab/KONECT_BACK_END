package gg.agit.konect.domain.notification.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.notification.model.NotificationInbox;
import gg.agit.konect.global.exception.CustomException;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_NOTIFICATION_INBOX;

public interface NotificationInboxRepository extends Repository<NotificationInbox, Integer> {

    NotificationInbox save(NotificationInbox notificationInbox);

    Page<NotificationInbox> findAllByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Integer userId);

    Optional<NotificationInbox> findByIdAndUserId(Integer id, Integer userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE NotificationInbox n SET n.isRead = true, n.updatedAt = CURRENT_TIMESTAMP "
        + "WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Integer userId);

    default NotificationInbox getByIdAndUserId(Integer id, Integer userId) {
        return findByIdAndUserId(id, userId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_NOTIFICATION_INBOX));
    }
}
