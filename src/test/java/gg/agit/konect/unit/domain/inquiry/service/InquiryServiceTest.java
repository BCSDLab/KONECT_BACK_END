package gg.agit.konect.unit.domain.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import gg.agit.konect.domain.inquiry.event.InquirySubmittedEvent;
import gg.agit.konect.domain.inquiry.service.InquiryService;
import gg.agit.konect.support.ServiceTestSupport;

class InquiryServiceTest extends ServiceTestSupport {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private InquiryService inquiryService;

    @Test
    @DisplayName("문의 내용을 원문 그대로 이벤트로 발행한다")
    void submitInquiryPublishesEventWithOriginalContent() {
        // given
        String content = "  앱 사용 중 오류가 발생했습니다.  ";

        // when
        inquiryService.submitInquiry(content);

        // then
        ArgumentCaptor<InquirySubmittedEvent> eventCaptor = ArgumentCaptor.forClass(InquirySubmittedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().content()).isEqualTo(content);
    }
}
