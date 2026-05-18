package gg.agit.konect.unit.infrastructure.slack.listener;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.inquiry.event.InquirySubmittedEvent;
import gg.agit.konect.infrastructure.slack.listener.InquirySlackListener;
import gg.agit.konect.infrastructure.slack.service.SlackNotificationService;
import gg.agit.konect.support.ServiceTestSupport;

class InquirySlackListenerTest extends ServiceTestSupport {

    @Mock
    private SlackNotificationService slackNotificationService;

    @InjectMocks
    private InquirySlackListener inquirySlackListener;

    @Test
    @DisplayName("문의 이벤트의 내용을 Slack 알림 서비스에 위임한다")
    void handleInquirySubmittedDelegatesContentToSlackService() {
        // given
        InquirySubmittedEvent event = InquirySubmittedEvent.from("앱 사용 중 오류가 발생했습니다.");

        // when
        inquirySlackListener.handleInquirySubmitted(event);

        // then
        verify(slackNotificationService).notifyInquiry(event.content());
    }
}
