package gg.agit.konect.domain.notification.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.notification.enums.NotificationInboxType;
import gg.agit.konect.domain.notification.model.NotificationInbox;
import gg.agit.konect.global.exception.CustomException;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_NOTIFICATION_INBOX;

public interface NotificationInboxRepository extends Repository<NotificationInbox, Integer> {

    NotificationInbox save(NotificationInbox notificationInbox);

    List<NotificationInbox> saveAll(Iterable<NotificationInbox> notificationInboxes);

    Page<NotificationInbox> findAllByUserIdAndTypeNotInOrderByCreatedAtDescIdDesc(
        Integer userId,
        Collection<NotificationInboxType> excludedTypes,
        Pageable pageable
    );

    long countByUserIdAndIsReadFalse(Integer userId);

    long countByUserIdAndIsReadFalseAndTypeNotIn(Integer userId, Collection<NotificationInboxType> excludedTypes);

    Optional<NotificationInbox> findByIdAndUserId(Integer id, Integer userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE NotificationInbox n
        SET n.isRead = true
        WHERE n.user.id = :userId
          AND n.isRead = false
          AND n.type NOT IN :excludedTypes
        """)
    void markAllAsReadByUserIdAndTypeNotIn(
        @Param("userId") Integer userId,
        @Param("excludedTypes") Collection<NotificationInboxType> excludedTypes
    );

    default NotificationInbox getByIdAndUserId(Integer id, Integer userId) {
        return findByIdAndUserId(id, userId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_NOTIFICATION_INBOX));
    }
}
