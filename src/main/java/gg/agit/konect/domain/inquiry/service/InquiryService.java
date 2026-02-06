package gg.agit.konect.domain.inquiry.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.inquiry.event.InquirySubmittedEvent;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void submitInquiry(String content) {
        applicationEventPublisher.publishEvent(InquirySubmittedEvent.from(content));
    }
}
