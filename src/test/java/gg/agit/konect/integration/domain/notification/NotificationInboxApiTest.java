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
        user = persist(UserFixture.createUser(university, "테스트유저", "2021136001"));
        otherUser = persist(UserFixture.createUser(university, "다른유저", "2021136002"));
    }

    private NotificationInbox createInbox(User owner, NotificationInboxType type, String title) {
        return persist(NotificationInbox.of(owner, type, title, "테스트 본문입니다.", "clubs/1"));
    }

    @Nested
    @DisplayName("GET /notifications/inbox - 인앱 알림 목록 조회")
    class GetMyInboxes {

        @Test
        @DisplayName("알림 목록을 최신순으로 조회한다")
        void getMyInboxesSuccess() throws Exception {
            // given
            NotificationInbox first = createInbox(user, NotificationInboxType.CLUB_APPLICATION_APPROVED, "동아리 승인");
            NotificationInbox second = createInbox(user, NotificationInboxType.CLUB_APPLICATION_REJECTED, "동아리 거절");
            clearPersistenceContext();
            mockLoginUser(user.getId());

            // when & then
            performGet("/notifications/inbox")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications").isArray())
                .andExpect(jsonPath("$.notifications.length()").value(2))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.notifications[0].id").value(second.getId()))
                .andExpect(jsonPath("$.notifications[1].id").value(first.getId()));
        }

        @Test
        @DisplayName("자신의 알림만 조회된다")
        void getMyInboxesOnlyMine() throws Exception {
            // given
            createInbox(user, NotificationInboxType.CLUB_APPLICATION_APPROVED, "내 알림");
            createInbox(otherUser, NotificationInboxType.CLUB_APPLICATION_APPROVED, "다른 유저 알림");
            clearPersistenceContext();
            mockLoginUser(user.getId());

            // when & then
            performGet("/notifications/inbox")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.length()").value(1))
                .andExpect(jsonPath("$.notifications[0].title").value("내 알림"));
        }

        @Test
        @DisplayName("page=0으로 요청하면 400을 반환한다")
        void getMyInboxesWithInvalidPageFails() throws Exception {
            // given
            mockLoginUser(user.getId());

            // when & then
            performGet("/notifications/inbox?page=0")
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("알림이 없으면 빈 목록을 반환한다")
        void getMyInboxesWhenEmptyReturnsEmptyList() throws Exception {
            // given
            mockLoginUser(user.getId());

            // when & then
            performGet("/notifications/inbox")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /notifications/inbox/unread-count - 미읽음 알림 개수 조회")
    class GetUnreadCount {

        @Test
        @DisplayName("미읽음 알림 개수를 반환한다")
        void getUnreadCountSuccess() throws Exception {
            // given
            createInbox(user, NotificationInboxType.CLUB_APPLICATION_APPROVED, "알림1");
            createInbox(user, NotificationInboxType.CLUB_APPLICATION_APPROVED, "알림2");
            clearPersistenceContext();
            mockLoginUser(user.getId());

            // when & then
            performGet("/notifications/inbox/unread-count")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));
        }

        @Test
        @DisplayName("알림이 없으면 미읽음 개수가 0이다")
        void getUnreadCountWhenNoneReturnsZero() throws Exception {
            // given
            mockLoginUser(user.getId());

            // when & then
            performGet("/notifications/inbox/unread-count")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
        }
    }

    @Nested
    @DisplayName("PATCH /notifications/inbox/{id}/read - 단건 읽음 처리")
    class MarkAsRead {

        @Test
        @DisplayName("알림을 읽음 처리한다")
        void markAsReadSuccess() throws Exception {
            // given
            NotificationInbox inbox = createInbox(user, NotificationInboxType.CLUB_APPLICATION_APPROVED, "읽을 알림");
            clearPersistenceContext();
            mockLoginUser(user.getId());

            // when & then
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
        @DisplayName("다른 유저의 알림을 읽음 처리하면 404를 반환한다")
        void markAsReadOtherUserInboxFails() throws Exception {
            // given
            NotificationInbox otherInbox = createInbox(
                otherUser, NotificationInboxType.CLUB_APPLICATION_APPROVED, "다른 유저 알림");
            clearPersistenceContext();
            mockLoginUser(user.getId());

            // when & then
            mockMvc.perform(patch("/notifications/inbox/" + otherInbox.getId() + "/read")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_NOTIFICATION_INBOX"));
        }
    }

    @Nested
    @DisplayName("PATCH /notifications/inbox/read-all - 전체 읽음 처리")
    class MarkAllAsRead {

        @Test
        @DisplayName("자신의 모든 알림을 읽음 처리한다")
        void markAllAsReadSuccess() throws Exception {
            // given
            createInbox(user, NotificationInboxType.CLUB_APPLICATION_APPROVED, "알림1");
            createInbox(user, NotificationInboxType.CLUB_APPLICATION_SUBMITTED, "알림2");
            clearPersistenceContext();
            mockLoginUser(user.getId());

            // when & then
            mockMvc.perform(patch("/notifications/inbox/read-all")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            clearPersistenceContext();
            long unreadCount = notificationInboxRepository.countByUserIdAndIsReadFalse(user.getId());
            assertThat(unreadCount).isZero();
        }

        @Test
        @DisplayName("전체 읽음 처리 후 미읽음 개수가 0이 된다")
        void markAllAsReadUpdatesUnreadCount() throws Exception {
            // given
            createInbox(user, NotificationInboxType.CLUB_APPLICATION_APPROVED, "알림1");
            createInbox(user, NotificationInboxType.CLUB_APPLICATION_REJECTED, "알림2");
            createInbox(otherUser, NotificationInboxType.CLUB_APPLICATION_APPROVED, "다른 유저 알림");
            clearPersistenceContext();
            mockLoginUser(user.getId());

            // when
            mockMvc.perform(patch("/notifications/inbox/read-all")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            // then: 내 알림만 읽음 처리됨
            clearPersistenceContext();
            assertThat(notificationInboxRepository.countByUserIdAndIsReadFalse(user.getId())).isZero();
            assertThat(notificationInboxRepository.countByUserIdAndIsReadFalse(otherUser.getId())).isEqualTo(1L);
        }
    }
}
