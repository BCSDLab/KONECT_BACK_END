package gg.agit.konect.domain.notification.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.notification.dto.NotificationInboxResponse;
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
    private final NotificationInboxSseService notificationInboxSseService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationInbox save(Integer userId, NotificationInboxType type, String title, String body, String path) {
        User user = userRepository.getById(userId);
        return notificationInboxRepository.save(NotificationInbox.of(user, type, title, body, path));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<NotificationInbox> saveAll(
        List<Integer> userIds,
        NotificationInboxType type,
        String title,
        String body,
        String path
    ) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        List<User> users = userRepository.findAllByIdIn(userIds);
        Map<Integer, User> userMap = users.stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

        return userIds.stream()
            .map(userMap::get)
            .filter(Objects::nonNull)
            .map(user -> NotificationInbox.of(user, type, title, body, path))
            .map(notificationInboxRepository::save)
            .toList();
    }

    public void sendSse(Integer userId, NotificationInboxResponse response) {
        try {
            notificationInboxSseService.send(userId, response);
        } catch (Exception e) {
            log.warn("Failed to send SSE notification: userId={}", userId, e);
        }
    }

    public void sendSseBatch(List<Integer> userIds, List<NotificationInbox> inboxes) {
        for (int i = 0; i < inboxes.size(); i++) {
            sendSse(userIds.get(i), NotificationInboxResponse.from(inboxes.get(i)));
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
