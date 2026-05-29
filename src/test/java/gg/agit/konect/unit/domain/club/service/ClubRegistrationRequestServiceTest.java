package gg.agit.konect.unit.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

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
import gg.agit.konect.domain.club.model.ClubInformationUpdateRequest;
import gg.agit.konect.domain.club.model.ClubRegistrationRequest;
import gg.agit.konect.domain.club.repository.ClubInformationUpdateRequestRepository;
import gg.agit.konect.domain.club.repository.ClubRegistrationRequestRepository;
import gg.agit.konect.domain.club.service.ClubRegistrationRequestService;
import gg.agit.konect.domain.website.model.WebClub;
import gg.agit.konect.domain.website.repository.WebsiteQueryRepository;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.WebClubFixture;
import gg.agit.konect.support.fixture.WebUniversityFixture;

class ClubRegistrationRequestServiceTest extends ServiceTestSupport {

    @Mock
    private ClubRegistrationRequestRepository clubRegistrationRequestRepository;

    @Mock
    private ClubInformationUpdateRequestRepository clubInformationUpdateRequestRepository;

    @Mock
    private WebsiteQueryRepository websiteQueryRepository;

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
        WebClub club = WebClubFixture.createWithId(
            1,
            WebUniversityFixture.createWithId(1),
            "현재 동아리명",
            ClubCategory.HOBBY
        );
        ClubInformationUpdateRequestDto request = new ClubInformationUpdateRequestDto(
            "한국기술교육대학교",
            "요청 동아리명",
            ClubCategory.ACADEMIC,
            "AI",
            "수정 소개",
            "수정 상세 소개입니다.",
            List.of("https://example.com/image1.jpg")
        );
        ClubInformationUpdateRequest saved = ClubInformationUpdateRequest.builder()
            .id(10)
            .club(club)
            .universityName(request.universityName())
            .clubName(request.clubName())
            .clubCategory(request.clubCategory())
            .clubTopic(request.clubTopic())
            .shortDescription(request.shortDescription())
            .fullIntroduction(request.fullIntroduction())
            .build();
        saved.addImages(request.imageUrls());
        given(websiteQueryRepository.findClub(club.getId())).willReturn(Optional.of(club));
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
        assertThat(event.currentUniversityName()).isEqualTo(club.getUniversity().getKoreanName());
        assertThat(event.requestedUniversityName()).isEqualTo(request.universityName());
        assertThat(event.currentClubName()).isEqualTo(club.getName());
        assertThat(event.requestedClubName()).isEqualTo(request.clubName());
        assertThat(event.currentCategory()).isEqualTo(club.getClubCategory().getDescription());
        assertThat(event.requestedCategory()).isEqualTo(request.clubCategory().getDescription());
        assertThat(event.currentTopic()).isEqualTo(club.getTopic());
        assertThat(event.requestedTopic()).isEqualTo(request.clubTopic());
        assertThat(event.currentDescription()).isEqualTo(club.getDescription());
        assertThat(event.requestedDescription()).isEqualTo(request.shortDescription());
        assertThat(event.currentFullIntroduction()).isEqualTo(club.getIntroduce());
        assertThat(event.requestedFullIntroduction()).isEqualTo(request.fullIntroduction());
        assertThat(event.currentImageUrl()).isEqualTo(club.getImageUrl());
        assertThat(event.requestedImageUrls()).containsExactlyElementsOf(request.imageUrls());
    }
}
