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

import gg.agit.konect.domain.club.dto.ClubInformationUpdateRequestDto;
import gg.agit.konect.domain.club.dto.ClubRegistrationRequestDto;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.event.ClubInformationUpdateRequestedEvent;
import gg.agit.konect.domain.club.event.ClubRegistrationRequestedEvent;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubInformationUpdateRequest;
import gg.agit.konect.domain.club.model.ClubRegistrationRequest;
import gg.agit.konect.domain.club.repository.ClubInformationUpdateRequestRepository;
import gg.agit.konect.domain.club.repository.ClubRegistrationRequestRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.club.service.ClubRegistrationRequestService;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.UniversityFixture;

class ClubRegistrationRequestServiceTest extends ServiceTestSupport {

    @Mock
    private ClubRegistrationRequestRepository clubRegistrationRequestRepository;

    @Mock
    private ClubInformationUpdateRequestRepository clubInformationUpdateRequestRepository;

    @Mock
    private ClubRepository clubRepository;

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

    @Test
    @DisplayName("동아리 정보 수정 요청 저장 후 Slack 알림 이벤트를 발행한다")
    void requestInformationUpdatePublishesClubInformationUpdateRequestedEvent() {
        // given
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), 1, "현재 동아리명");
        ClubInformationUpdateRequestDto request = new ClubInformationUpdateRequestDto(
            "요청 동아리명",
            ClubCategory.ACADEMIC,
            "수정 소개",
            "https://example.com/logo.png",
            "학생회관 102호",
            "수정 상세 소개입니다."
        );
        ClubInformationUpdateRequest saved = ClubInformationUpdateRequest.builder()
            .id(10)
            .club(club)
            .clubName(request.clubName())
            .clubCategory(request.clubCategory())
            .shortDescription(request.shortDescription())
            .imageUrl(request.imageUrl())
            .location(request.location())
            .fullIntroduction(request.fullIntroduction())
            .build();
        given(clubRepository.getById(club.getId())).willReturn(club);
        given(clubInformationUpdateRequestRepository.save(any(ClubInformationUpdateRequest.class))).willReturn(saved);

        // when
        clubRegistrationRequestService.requestInformationUpdate(club.getId(), request);

        // then
        ArgumentCaptor<ClubInformationUpdateRequestedEvent> eventCaptor = ArgumentCaptor.forClass(
            ClubInformationUpdateRequestedEvent.class
        );
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        ClubInformationUpdateRequestedEvent event = eventCaptor.getValue();
        assertThat(event.requestId()).isEqualTo(saved.getId());
        assertThat(event.clubId()).isEqualTo(club.getId());
        assertThat(event.currentClubName()).isEqualTo(club.getName());
        assertThat(event.requestedClubName()).isEqualTo(request.clubName());
        assertThat(event.category()).isEqualTo(request.clubCategory().getDescription());
        assertThat(event.description()).isEqualTo(request.shortDescription());
        assertThat(event.imageUrl()).isEqualTo(request.imageUrl());
        assertThat(event.location()).isEqualTo(request.location());
        assertThat(event.fullIntroduction()).isEqualTo(request.fullIntroduction());
    }
}
