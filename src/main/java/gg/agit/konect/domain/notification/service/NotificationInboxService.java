package gg.agit.konect.domain.notification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.notification.dto.NotificationInboxesResponse;
import gg.agit.konect.domain.notification.dto.NotificationInboxUnreadCountResponse;
import gg.agit.konect.domain.notification.enums.NotificationInboxType;
import gg.agit.konect.domain.notification.model.NotificationInbox;
import gg.agit.konect.domain.notification.repository.NotificationInboxRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationInboxService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final NotificationInboxRepository notificationInboxRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(Integer userId, NotificationInboxType type, String title, String body, String path) {
        try {
            User user = userRepository.getById(userId);
            NotificationInbox inbox = NotificationInbox.of(user, type, title, body, path);
            notificationInboxRepository.save(inbox);
        } catch (Exception e) {
            log.error("Failed to save notification inbox: userId={}, type={}", userId, type, e);
        }
    }

    public NotificationInboxesResponse getMyInboxes(Integer userId, int page) {
        PageRequest pageable = PageRequest.of(page - 1, DEFAULT_PAGE_SIZE);
        Page<NotificationInbox> result = notificationInboxRepository
            .findAllByUserIdOrderByCreatedAtDescIdDesc(userId, pageable);
        return NotificationInboxesResponse.from(result);
    }

    public NotificationInboxUnreadCountResponse getUnreadCount(Integer userId) {
        long count = notificationInboxRepository.countByUserIdAndIsReadFalse(userId);
        return NotificationInboxUnreadCountResponse.of(count);
    }

    @Transactional
    public void markAsRead(Integer userId, Integer notificationId) {
        NotificationInbox inbox = notificationInboxRepository.getByIdAndUserId(notificationId, userId);
        inbox.markAsRead();
    }

    @Transactional
    public void markAllAsRead(Integer userId) {
        notificationInboxRepository.markAllAsReadByUserId(userId);
    }
}
