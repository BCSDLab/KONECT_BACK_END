package gg.agit.konect.unit.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

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
