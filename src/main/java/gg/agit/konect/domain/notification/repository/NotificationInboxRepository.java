package gg.agit.konect.domain.notification.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.notification.model.NotificationInbox;
import gg.agit.konect.global.exception.CustomException;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_NOTIFICATION_INBOX;

public interface NotificationInboxRepository extends JpaRepository<NotificationInbox, Integer> {

    Page<NotificationInbox> findAllByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Integer userId);

    Optional<NotificationInbox> findByIdAndUserId(Integer id, Integer userId);

    @Modifying
    @Query("UPDATE NotificationInbox n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Integer userId);

    default NotificationInbox getByIdAndUserId(Integer id, Integer userId) {
        return findByIdAndUserId(id, userId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_NOTIFICATION_INBOX));
    }
}
