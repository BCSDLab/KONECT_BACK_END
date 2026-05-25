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

import gg.agit.konect.domain.club.dto.ClubRegistrationRequestDto;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.event.ClubRegistrationRequestedEvent;
import gg.agit.konect.domain.club.model.ClubRegistrationRequest;
import gg.agit.konect.domain.club.repository.ClubRegistrationRequestRepository;
import gg.agit.konect.domain.club.service.ClubRegistrationRequestService;
import gg.agit.konect.support.ServiceTestSupport;

class ClubRegistrationRequestServiceTest extends ServiceTestSupport {

    @Mock
    private ClubRegistrationRequestRepository clubRegistrationRequestRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ClubRegistrationRequestService clubRegistrationRequestService;

    @Test
    @DisplayName("동아리 등록 요청 저장 후 Slack 알림 이벤트를 발행한다")
    void registerPublishesClubRegistrationRequestedEvent() {
        // given
        ClubRegistrationRequestDto request = new ClubRegistrationRequestDto(
            "한국기술교육대학교",
            "BCSD Lab",
            ClubCategory.ACADEMIC,
            "코딩",
            "💻",
            "코딩 동아리입니다.",
            "상세한 동아리 소개 내용입니다.",
            List.of("https://example.com/image1.jpg")
        );
        ClubRegistrationRequest saved = ClubRegistrationRequest.builder()
            .id(1)
            .universityName(request.universityName())
            .clubName(request.clubName())
            .clubCategory(request.clubCategory())
            .clubTopic(request.clubTopic())
            .clubEmoji(request.clubEmoji())
            .shortDescription(request.shortDescription())
            .fullIntroduction(request.fullIntroduction())
            .build();
        saved.addImages(request.imageUrls());
        given(clubRegistrationRequestRepository.save(any(ClubRegistrationRequest.class))).willReturn(saved);

        // when
        clubRegistrationRequestService.register(request);

        // then
        ArgumentCaptor<ClubRegistrationRequestedEvent> eventCaptor = ArgumentCaptor.forClass(
            ClubRegistrationRequestedEvent.class
        );
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        ClubRegistrationRequestedEvent event = eventCaptor.getValue();
        assertThat(event.requestId()).isEqualTo(saved.getId());
        assertThat(event.universityName()).isEqualTo(request.universityName());
        assertThat(event.clubName()).isEqualTo(request.clubName());
        assertThat(event.category()).isEqualTo(request.clubCategory().getDescription());
        assertThat(event.topic()).isEqualTo(request.clubTopic());
        assertThat(event.emoji()).isEqualTo(request.clubEmoji());
        assertThat(event.description()).isEqualTo(request.shortDescription());
        assertThat(event.fullIntroduction()).isEqualTo(request.fullIntroduction());
        assertThat(event.imageUrls()).containsExactlyElementsOf(request.imageUrls());
        assertThat(event.imageCount()).isEqualTo(1);
    }
}
