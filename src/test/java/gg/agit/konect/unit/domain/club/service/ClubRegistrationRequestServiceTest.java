package gg.agit.konect.unit.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import gg.agit.konect.domain.club.dto.ClubRegistrationRequest;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.event.ClubRegistrationRequestedEvent;
import gg.agit.konect.domain.club.model.ClubRegistrationRequestEntity;
import gg.agit.konect.domain.club.repository.ClubRegistrationRequestRepository;
import gg.agit.konect.domain.club.service.ClubRegistrationRequestService;
import gg.agit.konect.support.ServiceTestSupport;

class ClubRegistrationRequestServiceTest extends ServiceTestSupport {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private ClubRegistrationRequestRepository clubRegistrationRequestRepository;

    @InjectMocks
    private ClubRegistrationRequestService clubRegistrationRequestService;

    @Test
    @DisplayName("신규 동아리 등록 요청 내용을 이벤트로 발행한다")
    void submitClubRegistrationRequestPublishesEvent() {
        // given
        ClubRegistrationRequest request = new ClubRegistrationRequest(
            "한국기술교육대학교",
            "BCSD Lab",
            ClubCategory.ACADEMIC,
            "개발",
            "💻",
            "즐겁게 서비스 만드는 동아리",
            List.of("https://example.com/club-1.png", "https://example.com/club-2.mp4"),
            "BCSD Lab은 IT 서비스 개발 동아리입니다."
        );
        given(clubRegistrationRequestRepository.save(any(ClubRegistrationRequestEntity.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // when
        clubRegistrationRequestService.submitClubRegistrationRequest(request);

        // then
        ArgumentCaptor<ClubRegistrationRequestEntity> entityCaptor =
            ArgumentCaptor.forClass(ClubRegistrationRequestEntity.class);
        verify(clubRegistrationRequestRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getClubName()).isEqualTo("BCSD Lab");
        assertThat(entityCaptor.getValue().getMediaUrls())
            .containsExactly("https://example.com/club-1.png", "https://example.com/club-2.mp4");

        ArgumentCaptor<ClubRegistrationRequestedEvent> eventCaptor =
            ArgumentCaptor.forClass(ClubRegistrationRequestedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(ClubRegistrationRequestedEvent.from(request));
    }
}
