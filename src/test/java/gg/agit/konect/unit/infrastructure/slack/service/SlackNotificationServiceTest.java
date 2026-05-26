package gg.agit.konect.unit.infrastructure.slack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import gg.agit.konect.infrastructure.slack.client.SlackClient;
import gg.agit.konect.infrastructure.slack.config.SlackProperties;
import gg.agit.konect.infrastructure.slack.service.SlackNotificationService;
import gg.agit.konect.support.ServiceTestSupport;

class SlackNotificationServiceTest extends ServiceTestSupport {

    private static final String ERROR_WEBHOOK_URL = "https://hooks.slack.com/error";
    private static final String EVENT_WEBHOOK_URL = "https://hooks.slack.com/event";

    @Mock
    private SlackClient slackClient;

    private SlackNotificationService slackNotificationService;

    @BeforeEach
    void setUp() {
        SlackProperties slackProperties = new SlackProperties(
            new SlackProperties.Webhooks(ERROR_WEBHOOK_URL, EVENT_WEBHOOK_URL),
            "signing-secret",
            "bot-token"
        );
        slackNotificationService = new SlackNotificationService(slackProperties, slackClient);
    }

    @Test
    @DisplayName("동아리 등록 요청 Slack 메시지를 마크다운과 이모지로 구성한다")
    void notifyClubRegistrationRequestFormatsSlackMessageWithMarkdownAndEmoji() {
        // when
        slackNotificationService.notifyClubRegistrationRequest(
            1,
            "한국기술교육대학교",
            "BCSD Lab",
            "학술",
            "코딩",
            "💻",
            "코딩 동아리입니다.",
            "상세한 동아리 소개 내용입니다.",
            List.of("https://example.com/image1.jpg", "https://example.com/image2.jpg")
        );

        // then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(slackClient).sendMessage(messageCaptor.capture(), eq(EVENT_WEBHOOK_URL));
        assertThat(messageCaptor.getValue()).isEqualTo(
            """
                :sparkles: *새 동아리 등록 요청이 도착했어요*

                :school: *대학교* : *`한국기술교육대학교`*
                💻 *동아리* : *`BCSD Lab`*
                :label: *분과* : *`학술`*
                :dart: *주제* : *`코딩`*
                :art: *요청 이모지* : *`💻`*

                :memo: *한 줄 소개*
                ```코딩 동아리입니다.```

                :page_facing_up: *상세 소개*
                ```상세한 동아리 소개 내용입니다.```

                :paperclip: *첨부 이미지*
                ```https://example.com/image1.jpg
                https://example.com/image2.jpg```
                """
        );
    }

    @Test
    @DisplayName("동아리 정보 수정 요청 Slack 메시지를 마크다운과 이모지로 구성한다")
    void notifyClubInformationUpdateRequestFormatsSlackMessageWithMarkdownAndEmoji() {
        // when
        slackNotificationService.notifyClubInformationUpdateRequest(
            1,
            2,
            "한국기술교육대학교",
            "한국기술교육대학교",
            "현재 동아리명",
            "요청 동아리명",
            "문화",
            "학술",
            "코딩",
            "AI",
            "🤖",
            "현재 소개",
            "수정 소개",
            "현재 상세 소개 내용입니다.",
            "수정 상세 소개 내용입니다.",
            "https://example.com/current-logo.png",
            List.of("https://example.com/image1.jpg")
        );

        // then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(slackClient).sendMessage(messageCaptor.capture(), eq(EVENT_WEBHOOK_URL));
        assertThat(messageCaptor.getValue()).isEqualTo(
            """
                :pencil2: *동아리 정보 수정 요청이 도착했어요*

                :receipt: *요청 ID* : *`1`*
                :id: *동아리 ID* : *`2`*
                :school: *대학교* : *`한국기술교육대학교`* → *`한국기술교육대학교`*
                :bookmark: *동아리명* : *`현재 동아리명`* → *`요청 동아리명`*
                :label: *분과* : *`문화`* → *`학술`*
                :dart: *주제* : *`코딩`* → *`AI`*
                :art: *요청 이모지* : *`🤖`*

                :memo: *한 줄 소개*
                ```현재 소개```
                →
                ```수정 소개```

                :page_facing_up: *상세 소개*
                ```현재 상세 소개 내용입니다.```
                →
                ```수정 상세 소개 내용입니다.```

                :frame_with_picture: *현재 대표 이미지*
                ```https://example.com/current-logo.png```

                :paperclip: *요청 첨부 이미지*
                ```https://example.com/image1.jpg```
                """
        );
    }
}
