package gg.agit.konect.unit.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.notification.dto.NotificationInboxResponse;
import gg.agit.konect.domain.notification.enums.NotificationInboxType;
import gg.agit.konect.domain.notification.model.NotificationInbox;
import gg.agit.konect.domain.notification.repository.NotificationInboxRepository;
import gg.agit.konect.domain.notification.service.NotificationInboxService;
import gg.agit.konect.domain.notification.service.NotificationInboxSseService;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;

class NotificationInboxServiceTest extends ServiceTestSupport {

    @Mock
    private NotificationInboxRepository notificationInboxRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationInboxSseService notificationInboxSseService;

    @InjectMocks
    private NotificationInboxService notificationInboxService;

    @Test
    @DisplayName("saveAll은 조회된 사용자에게만 알림을 생성한다")
    void saveAllOnlyCreatesForResolvedUsers() {
        // given
        University university = UniversityFixture.create();
        User user1 = createUser(university, 1, "유저1", "2021136001");
        User user2 = createUser(university, 2, "유저2", "2021136002");
        given(userRepository.findAllByIdIn(List.of(1, 2, 3))).willReturn(List.of(user1, user2));
        given(notificationInboxRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        List<NotificationInbox> result = notificationInboxService.saveAll(
            List.of(1, 2, 3),
            NotificationInboxType.CLUB_APPLICATION_APPROVED,
            "제목",
            "본문",
            "/clubs/1"
        );

        // then
        verify(notificationInboxRepository).saveAll(any());
        assertThatCode(() -> result.get(0).getTitle()).doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThat(result).hasSize(2);
        org.assertj.core.api.Assertions.assertThat(result)
            .extracting(inbox -> inbox.getUser().getStudentNumber())
            .containsExactly("2021136001", "2021136002");
    }

    @Test
    @DisplayName("sendSseBatch는 일부 사용자 전송 실패가 있어도 나머지 전송을 계속한다")
    void sendSseBatchContinuesWhenOneSendFails() {
        // given
        University university = UniversityFixture.create();
        User user1 = createUser(university, 1, "유저1", "2021136001");
        User user2 = createUser(university, 2, "유저2", "2021136002");
        NotificationInbox inbox1 = NotificationInbox.of(
            user1,
            NotificationInboxType.CLUB_APPLICATION_APPROVED,
            "제목1",
            "본문1",
            "/clubs/1"
        );
        NotificationInbox inbox2 = NotificationInbox.of(
            user2,
            NotificationInboxType.CLUB_APPLICATION_REJECTED,
            "제목2",
            "본문2",
            "/clubs/2"
        );

        doThrow(new RuntimeException("sse failure"))
            .when(notificationInboxSseService)
            .send(eq(user1.getId()), any(NotificationInboxResponse.class));

        // when
        assertThatCode(() -> notificationInboxService.sendSseBatch(List.of(inbox1, inbox2)))
            .doesNotThrowAnyException();

        // then
        verify(notificationInboxSseService, times(1)).send(eq(user1.getId()), any(NotificationInboxResponse.class));
        verify(notificationInboxSseService, times(1)).send(eq(user2.getId()), any(NotificationInboxResponse.class));
    }

    @Test
    @DisplayName("saveAll은 대상 사용자가 없으면 저장을 시도하지 않는다")
    void saveAllSkipsWhenUserIdsEmpty() {
        // when
        List<NotificationInbox> result = notificationInboxService.saveAll(
            List.of(),
            NotificationInboxType.CLUB_APPLICATION_APPROVED,
            "제목",
            "본문",
            "/clubs/1"
        );

        // then
        org.assertj.core.api.Assertions.assertThat(result).isEmpty();
        verify(userRepository, never()).findAllByIdIn(any());
        verify(notificationInboxRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("save는 단일 알림을 생성한다")
    void saveCreatesSingleNotification() {
        // given
        University university = UniversityFixture.create();
        User user = createUser(university, 1, "유저1", "2021136001");
        NotificationInbox inbox = NotificationInbox.of(
            user,
            NotificationInboxType.CLUB_APPLICATION_APPROVED,
            "제목",
            "본문",
            "/clubs/1"
        );
        given(userRepository.getById(1)).willReturn(user);
        given(notificationInboxRepository.save(any(NotificationInbox.class))).willReturn(inbox);

        // when
        NotificationInbox result = notificationInboxService.save(
            1,
            NotificationInboxType.CLUB_APPLICATION_APPROVED,
            "제목",
            "본문",
            "/clubs/1"
        );

        // then
        verify(notificationInboxRepository).save(any(NotificationInbox.class));
        org.assertj.core.api.Assertions.assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("sendSse는 SSE 알림을 발송한다")
    void sendSseSendsNotification() {
        // given
        University university = UniversityFixture.create();
        User user = createUser(university, 1, "유저1", "2021136001");
        NotificationInbox inbox = NotificationInbox.of(
            user,
            NotificationInboxType.CLUB_APPLICATION_APPROVED,
            "제목",
            "본문",
            "/clubs/1"
        );
        NotificationInboxResponse response = NotificationInboxResponse.from(inbox);
        doNothing().when(notificationInboxSseService).send(eq(1), any(NotificationInboxResponse.class));

        // when
        assertThatCode(() -> notificationInboxService.sendSse(1, response))
            .doesNotThrowAnyException();

        // then
        verify(notificationInboxSseService).send(eq(1), any(NotificationInboxResponse.class));
    }

    @Test
    @DisplayName("getMyInboxes는 빈 결과를 반환한다")
    void getMyInboxesReturnsEmptyResult() {
        // given
        org.springframework.data.domain.Page<NotificationInbox> emptyPage = org.springframework.data.domain.Page.empty();
        given(notificationInboxRepository.findAllByUserIdAndTypeNotInOrderByCreatedAtDescIdDesc(
            eq(1),
            any(Set.class),
            any(org.springframework.data.domain.PageRequest.class)
        )).willReturn(emptyPage);

        // when
        gg.agit.konect.domain.notification.dto.NotificationInboxesResponse result =
            notificationInboxService.getMyInboxes(1, 1);

        // then
        org.assertj.core.api.Assertions.assertThat(result.notifications()).isEmpty();
    }

    @Test
    @DisplayName("getUnreadCount는 카운트 0을 반환한다")
    void getUnreadCountReturnsZero() {
        // given
        given(notificationInboxRepository.countByUserIdAndIsReadFalseAndTypeNotIn(
            eq(1),
            any(Set.class)
        )).willReturn(0L);

        // when
        gg.agit.konect.domain.notification.dto.NotificationInboxUnreadCountResponse result =
            notificationInboxService.getUnreadCount(1);

        // then
        org.assertj.core.api.Assertions.assertThat(result.unreadCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("markAsRead는 정상 동작한다")
    void markAsReadWorksNormally() {
        // given
        University university = UniversityFixture.create();
        User user = createUser(university, 1, "유저1", "2021136001");
        NotificationInbox inbox = NotificationInbox.of(
            user,
            NotificationInboxType.CLUB_APPLICATION_APPROVED,
            "제목",
            "본문",
            "/clubs/1"
        );
        given(notificationInboxRepository.getByIdAndUserId(1, 1)).willReturn(inbox);

        // when
        assertThatCode(() -> notificationInboxService.markAsRead(1, 1))
            .doesNotThrowAnyException();

        // then
        org.assertj.core.api.Assertions.assertThat(inbox.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("markAllAsRead는 알림이 없는 경우에도 에러 없이 동작한다")
    void markAllAsReadWorksWithNoNotifications() {
        // given
        doNothing().when(notificationInboxRepository).markAllAsReadByUserIdAndTypeNotIn(
            eq(1),
            any(Set.class)
        );

        // when
        assertThatCode(() -> notificationInboxService.markAllAsRead(1))
            .doesNotThrowAnyException();

        // then
        verify(notificationInboxRepository).markAllAsReadByUserIdAndTypeNotIn(eq(1), any(Set.class));
    }

    @Test
    @DisplayName("saveAll은 일부 사용자만 존재하면 존재하는 사용자에게만 알림을 생성한다")
    void saveAllCreatesNotificationsOnlyForExistingUsers() {
        // given
        University university = UniversityFixture.create();
        User user1 = createUser(university, 1, "유저1", "2021136001");
        User user3 = createUser(university, 3, "유저3", "2021136003");
        given(userRepository.findAllByIdIn(List.of(1, 2, 3))).willReturn(List.of(user1, user3));
        given(notificationInboxRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        List<NotificationInbox> result = notificationInboxService.saveAll(
            List.of(1, 2, 3),
            NotificationInboxType.CLUB_APPLICATION_APPROVED,
            "제목",
            "본문",
            "/clubs/1"
        );

        // then
        verify(notificationInboxRepository).saveAll(any());
        org.assertj.core.api.Assertions.assertThat(result).hasSize(2);
        org.assertj.core.api.Assertions.assertThat(result)
            .extracting(inbox -> inbox.getUser().getId())
            .containsExactly(1, 3);
    }

    @Test
    @DisplayName("markAsRead는 다른 사용자의 알림에 대해 예외를 발생시킨다")
    void markAsReadThrowsExceptionForOtherUsersNotification() {
        // given
        University university = UniversityFixture.create();
        createUser(university, 1, "유저1", "2021136001");
        createUser(university, 2, "유저2", "2021136002");
        given(notificationInboxRepository.getByIdAndUserId(1, 2)).willThrow(
            new org.springframework.dao.EmptyResultDataAccessException(1)
        );

        // when & then
        assertThatThrownBy(() -> notificationInboxService.markAsRead(2, 1))
            .isInstanceOf(org.springframework.dao.EmptyResultDataAccessException.class);
    }

    private User createUser(University university, Integer id, String name, String studentNumber) {
        return User.builder()
            .id(id)
            .university(university)
            .email(studentNumber + "@koreatech.ac.kr")
            .name(name)
            .studentNumber(studentNumber)
            .role(UserRole.USER)
            .isMarketingAgreement(true)
            .imageUrl("https://example.com/profile.png")
            .build();
    }
}
