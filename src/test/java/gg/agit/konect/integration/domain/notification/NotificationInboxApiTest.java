package gg.agit.konect.integration.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import gg.agit.konect.domain.notification.enums.NotificationInboxType;
import gg.agit.konect.domain.notification.model.NotificationInbox;
import gg.agit.konect.domain.notification.repository.NotificationInboxRepository;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class NotificationInboxApiTest extends IntegrationTestSupport {

    @Autowired
    private NotificationInboxRepository notificationInboxRepository;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        University university = persist(UniversityFixture.create());
        user = persist(UserFixture.createUser(university, "test-user", "2021136001"));
        otherUser = persist(UserFixture.createUser(university, "other-user", "2021136002"));
    }

    private NotificationInbox createInbox(User owner, NotificationInboxType type, String title) {
        return persist(NotificationInbox.of(owner, type, title, "test-body", "clubs/1"));
    }

    @Nested
    @DisplayName("GET /notifications/inbox")
    class GetMyInboxes {

        @Test
        @DisplayName("returns notifications in latest-first order")
        void getMyInboxesSuccess() throws Exception {
            NotificationInbox first = createInbox(user, NotificationInboxType.CLUB_APPLICATION_APPROVED, "approved");
            NotificationInbox second = createInbox(user, NotificationInboxType.CLUB_APPLICATION_REJECTED, "rejected");
            clearPersistenceContext();
            mockLoginUser(user.getId());

            performGet("/notifications/inbox")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications").isArray())
                .andExpect(jsonPath("$.notifications.length()").value(2))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.notifications[0].id").value(second.getId()))
                .andExpect(jsonPath("$.notifications[1].id").value(first.getId()));
        }

        @Test
        @DisplayName("returns only my notifications")
        void getMyInboxesOnlyMine() throws Exception {
            createInbox(user, NotificationInboxType.CLUB_APPLICATION_APPROVED, "my-notification");
            createInbox(otherUser, NotificationInboxType.CLUB_APPLICATION_APPROVED, "other-notification");
            clearPersistenceContext();
            mockLoginUser(user.getId());

            performGet("/notifications/inbox")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.length()").value(1))
                .andExpect(jsonPath("$.notifications[0].title").value("my-notification"));
        }

        @Test
        @DisplayName("excludes chat-related in-app notifications from the list")
        void getMyInboxesExcludesChatNotifications() throws Exception {
            createInbox(user, NotificationInboxType.CHAT_MESSAGE, "chat-notification");
            createInbox(user, NotificationInboxType.CLUB_APPLICATION_APPROVED, "club-notification");
            clearPersistenceContext();
            mockLoginUser(user.getId());

            performGet("/notifications/inbox")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.length()").value(1))
                .andExpect(jsonPath("$.notifications[0].type").value("CLUB_APPLICATION_APPROVED"))
                .andExpect(jsonPath("$.notifications[0].title").value("club-notification"));
        }

        @Test
        @DisplayName("returns 400 when page is zero")
        void getMyInboxesWithInvalidPageFails() throws Exception {
            mockLoginUser(user.getId());

            performGet("/notifications/inbox?page=0")
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns empty list when there are no notifications")
        void getMyInboxesWhenEmptyReturnsEmptyList() throws Exception {
            mockLoginUser(user.getId());

            performGet("/notifications/inbox")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /notifications/inbox/unread-count")
    class GetUnreadCount {

        @Test
        @DisplayName("returns unread notification count")
        void getUnreadCountSuccess() throws Exception {
            createInbox(user, NotificationInboxType.CLUB_APPLICATION_APPROVED, "notification-1");
            createInbox(user, NotificationInboxType.CLUB_APPLICATION_APPROVED, "notification-2");
            clearPersistenceContext();
            mockLoginUser(user.getId());

            performGet("/notifications/inbox/unread-count")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));
        }

        @Test
        @DisplayName("returns zero when there are no notifications")
        void getUnreadCountWhenNoneReturnsZero() throws Exception {
            mockLoginUser(user.getId());

            performGet("/notifications/inbox/unread-count")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
        }

        @Test
        @DisplayName("excludes chat-related in-app notifications from unread count")
        void getUnreadCountExcludesChatNotifications() throws Exception {
            createInbox(user, NotificationInboxType.CHAT_MESSAGE, "chat-notification");
            createInbox(user, NotificationInboxType.GROUP_CHAT_MESSAGE, "group-chat-notification");
            createInbox(user, NotificationInboxType.CLUB_APPLICATION_APPROVED, "club-notification");
            clearPersistenceContext();
            mockLoginUser(user.getId());

            performGet("/notifications/inbox/unread-count")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
        }
    }

    @Nested
    @DisplayName("PATCH /notifications/inbox/{id}/read")
    class MarkAsRead {

        @Test
        @DisplayName("marks one notification as read")
        void markAsReadSuccess() throws Exception {
            NotificationInbox inbox = createInbox(user, NotificationInboxType.CLUB_APPLICATION_APPROVED, "read-me");
            clearPersistenceContext();
            mockLoginUser(user.getId());

            mockMvc.perform(patch("/notifications/inbox/" + inbox.getId() + "/read")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            clearPersistenceContext();
            assertThat(notificationInboxRepository.findByIdAndUserId(inbox.getId(), user.getId()))
                .isPresent()
                .get()
                .extracting(NotificationInbox::getIsRead)
                .isEqualTo(true);
        }

        @Test
        @DisplayName("returns 404 for another user's notification")
        void markAsReadOtherUserInboxFails() throws Exception {
            NotificationInbox otherInbox = createInbox(
                otherUser,
                NotificationInboxType.CLUB_APPLICATION_APPROVED,
                "other-notification"
            );
            clearPersistenceContext();
            mockLoginUser(user.getId());

            mockMvc.perform(patch("/notifications/inbox/" + otherInbox.getId() + "/read")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_NOTIFICATION_INBOX"));
        }
    }

    @Nested
    @DisplayName("PATCH /notifications/inbox/read-all")
    class MarkAllAsRead {

        @Test
        @DisplayName("marks all visible notifications as read")
        void markAllAsReadSuccess() throws Exception {
            createInbox(user, NotificationInboxType.CLUB_APPLICATION_APPROVED, "notification-1");
            createInbox(user, NotificationInboxType.CLUB_APPLICATION_SUBMITTED, "notification-2");
            clearPersistenceContext();
            mockLoginUser(user.getId());

            mockMvc.perform(patch("/notifications/inbox/read-all")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            clearPersistenceContext();
            long unreadCount = notificationInboxRepository.countByUserIdAndIsReadFalse(user.getId());
            assertThat(unreadCount).isZero();
        }

        @Test
        @DisplayName("does not affect another user's unread count")
        void markAllAsReadUpdatesUnreadCount() throws Exception {
            createInbox(user, NotificationInboxType.CLUB_APPLICATION_APPROVED, "notification-1");
            createInbox(user, NotificationInboxType.CLUB_APPLICATION_REJECTED, "notification-2");
            createInbox(otherUser, NotificationInboxType.CLUB_APPLICATION_APPROVED, "other-notification");
            clearPersistenceContext();
            mockLoginUser(user.getId());

            mockMvc.perform(patch("/notifications/inbox/read-all")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            clearPersistenceContext();
            assertThat(notificationInboxRepository.countByUserIdAndIsReadFalse(user.getId())).isZero();
            assertThat(notificationInboxRepository.countByUserIdAndIsReadFalse(otherUser.getId())).isEqualTo(1L);
        }
    }
}
