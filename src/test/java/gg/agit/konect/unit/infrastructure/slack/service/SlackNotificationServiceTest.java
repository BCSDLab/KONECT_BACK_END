package gg.agit.konect.unit.infrastructure.slack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import gg.agit.konect.domain.club.dto.ClubRegistrationRequest;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.event.ClubRegistrationRequestedEvent;
import gg.agit.konect.infrastructure.slack.client.SlackClient;
import gg.agit.konect.infrastructure.slack.config.SlackProperties;
import gg.agit.konect.infrastructure.slack.service.SlackNotificationService;

class SlackNotificationServiceTest {

    @Test
    @DisplayName("신규 동아리 등록 요청을 가입/탈퇴 알림과 같은 event webhook으로 전송한다")
    void notifyClubRegistrationRequestSendsMessageToEventWebhook() {
        // given
        SlackProperties slackProperties = new SlackProperties(
            new SlackProperties.Webhooks("https://hooks.slack.com/error", "https://hooks.slack.com/event"),
            "secret",
            "bot-token"
        );
        SlackClient slackClient = mock(SlackClient.class);
        SlackNotificationService slackNotificationService =
            new SlackNotificationService(slackProperties, slackClient);
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
        slackNotificationService.notifyClubRegistrationRequest(event);

        // then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(slackClient).sendMessage(messageCaptor.capture(), eq("https://hooks.slack.com/event"));
        assertThat(messageCaptor.getValue())
            .contains("신규 동아리 등록 요청")
            .contains("한국기술교육대학교")
            .contains("BCSD Lab")
            .contains("학술")
            .contains("https://example.com/club-1.png");
    }
}
