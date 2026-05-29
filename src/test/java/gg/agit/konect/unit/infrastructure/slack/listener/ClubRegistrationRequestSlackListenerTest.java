package gg.agit.konect.unit.infrastructure.slack.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.club.event.ClubInformationUpdateRequestedEvent;
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
            event.fullIntroduction(),
            event.imageUrls()
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
                event.fullIntroduction(),
                event.imageUrls()
            );

        // when & then
        assertThatCode(() -> clubRegistrationRequestSlackListener.handleClubRegistrationRequested(event))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("동아리 정보 수정 요청 이벤트를 Slack 알림 서비스에 위임한다")
    void handleClubInformationUpdateRequestedDelegatesToSlackService() {
        // given
        ClubInformationUpdateRequestedEvent event = createInformationUpdateEvent();

        // when
        clubRegistrationRequestSlackListener.handleClubInformationUpdateRequested(event);

        // then
        verify(slackNotificationService).notifyClubInformationUpdateRequest(
            event.requestId(),
            event.clubId(),
            event.currentUniversityName(),
            event.requestedUniversityName(),
            event.currentClubName(),
            event.requestedClubName(),
            event.currentCategory(),
            event.requestedCategory(),
            event.currentTopic(),
            event.requestedTopic(),
            event.currentDescription(),
            event.requestedDescription(),
            event.currentFullIntroduction(),
            event.requestedFullIntroduction(),
            event.currentCategoryEmoji(),
            event.requestedImageUrls()
        );
    }

    @Test
    @DisplayName("동아리 정보 수정 요청 Slack 알림 실패를 삼킨다")
    void handleClubInformationUpdateRequestedSwallowsExceptions() {
        // given
        ClubInformationUpdateRequestedEvent event = createInformationUpdateEvent();
        doThrow(new RuntimeException("slack error"))
            .when(slackNotificationService)
            .notifyClubInformationUpdateRequest(
                event.requestId(),
                event.clubId(),
                event.currentUniversityName(),
                event.requestedUniversityName(),
                event.currentClubName(),
                event.requestedClubName(),
                event.currentCategory(),
                event.requestedCategory(),
                event.currentTopic(),
                event.requestedTopic(),
                event.currentDescription(),
                event.requestedDescription(),
                event.currentFullIntroduction(),
                event.requestedFullIntroduction(),
                event.currentCategoryEmoji(),
                event.requestedImageUrls()
            );

        // when & then
        assertThatCode(() -> clubRegistrationRequestSlackListener.handleClubInformationUpdateRequested(event))
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
            "상세한 동아리 소개 내용입니다.",
            List.of("https://example.com/image1.jpg")
        );
    }

    private ClubInformationUpdateRequestedEvent createInformationUpdateEvent() {
        return new ClubInformationUpdateRequestedEvent(
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
            "현재 소개",
            "수정 소개",
            "현재 상세 소개 내용입니다.",
            "수정 상세 소개 내용입니다.",
            "https://example.com/current-logo.png",
            List.of("https://example.com/image1.jpg")
        );
    }
}
