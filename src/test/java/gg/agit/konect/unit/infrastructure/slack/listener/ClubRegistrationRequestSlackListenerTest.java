package gg.agit.konect.unit.infrastructure.slack.listener;

import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.club.dto.ClubRegistrationRequest;
import gg.agit.konect.domain.club.enums.ClubCategory;
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
    @DisplayName("신규 동아리 등록 요청 이벤트를 Slack 알림 서비스에 위임한다")
    void handleClubRegistrationRequestedDelegatesEventToSlackService() {
        // given
        ClubRegistrationRequestedEvent event = ClubRegistrationRequestedEvent.from(new ClubRegistrationRequest(
            "한국기술교육대학교",
            "BCSD Lab",
            ClubCategory.ACADEMIC,
            "개발",
            "💻",
            "즐겁게 서비스 만드는 동아리",
            List.of("https://example.com/club-1.png"),
            "BCSD Lab은 IT 서비스 개발 동아리입니다."
        ));

        // when
        clubRegistrationRequestSlackListener.handleClubRegistrationRequested(event);

        // then
        verify(slackNotificationService).notifyClubRegistrationRequest(event);
    }
}
