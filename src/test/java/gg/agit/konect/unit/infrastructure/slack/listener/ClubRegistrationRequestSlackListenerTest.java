package gg.agit.konect.unit.infrastructure.slack.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.club.event.ClubRegistrationRequestedEvent;
import gg.agit.konect.infrastructure.slack.listener.ClubRegistrationRequestSlackListener;
import gg.agit.konect.infrastructure.slack.service.SlackNotificationService;
import gg.agit.konect.support.ServiceTestSupport;

class ClubRegistrationRequestSlackListenerTest extends ServiceTestSupport {

    @Mock
    private SlackNotificationService slackNotificationService;

    @InjectMocks
    private ClubRegistrationRequestSlackListener clubRegistrationRequestSlackListener;

    @Test
    @DisplayName("동아리 등록 요청 이벤트를 Slack 알림 서비스에 위임한다")
    void handleClubRegistrationRequestedDelegatesToSlackService() {
        // given
        ClubRegistrationRequestedEvent event = createEvent();

        // when
        clubRegistrationRequestSlackListener.handleClubRegistrationRequested(event);

        // then
        verify(slackNotificationService).notifyClubRegistrationRequest(
            event.requestId(),
            event.universityName(),
            event.clubName(),
            event.category(),
            event.topic(),
            event.emoji(),
            event.description(),
            event.imageCount()
        );
    }

    @Test
    @DisplayName("Slack 알림 실패가 이벤트 처리 밖으로 전파되지 않는다")
    void handleClubRegistrationRequestedSwallowsExceptions() {
        // given
        ClubRegistrationRequestedEvent event = createEvent();
        doThrow(new RuntimeException("slack error"))
            .when(slackNotificationService)
            .notifyClubRegistrationRequest(
                event.requestId(),
                event.universityName(),
                event.clubName(),
                event.category(),
                event.topic(),
                event.emoji(),
                event.description(),
                event.imageCount()
            );

        // when & then
        assertThatCode(() -> clubRegistrationRequestSlackListener.handleClubRegistrationRequested(event))
            .doesNotThrowAnyException();
    }

    private ClubRegistrationRequestedEvent createEvent() {
        return new ClubRegistrationRequestedEvent(
            1,
            "한국기술교육대학교",
            "BCSD Lab",
            "학술",
            "코딩",
            "💻",
            "코딩 동아리입니다.",
            1
        );
    }
}
