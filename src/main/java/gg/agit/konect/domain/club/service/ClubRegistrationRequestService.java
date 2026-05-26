package gg.agit.konect.domain.club.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.dto.ClubInformationUpdateRequestDto;
import gg.agit.konect.domain.club.dto.ClubRegistrationRequestDto;
import gg.agit.konect.domain.club.event.ClubInformationUpdateRequestedEvent;
import gg.agit.konect.domain.club.event.ClubRegistrationRequestedEvent;
import gg.agit.konect.domain.club.model.ClubInformationUpdateRequest;
import gg.agit.konect.domain.club.model.ClubRegistrationRequest;
import gg.agit.konect.domain.club.repository.ClubInformationUpdateRequestRepository;
import gg.agit.konect.domain.club.repository.ClubRegistrationRequestRepository;
import gg.agit.konect.domain.website.model.WebClub;
import gg.agit.konect.domain.website.repository.WebsiteQueryRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ClubRegistrationRequestService {

    private final ClubRegistrationRequestRepository clubRegistrationRequestRepository;
    private final ClubInformationUpdateRequestRepository clubInformationUpdateRequestRepository;
    private final WebsiteQueryRepository websiteQueryRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void register(ClubRegistrationRequestDto request) {
        ClubRegistrationRequest entity = ClubRegistrationRequest.builder()
            .universityName(request.universityName())
            .clubName(request.clubName())
            .clubCategory(request.clubCategory())
            .clubTopic(request.clubTopic())
            .clubEmoji(request.clubEmoji())
            .shortDescription(request.shortDescription())
            .fullIntroduction(request.fullIntroduction())
            .status(ClubRegistrationRequest.RegistrationStatus.PENDING)
            .build();

        // 이미지 추가
        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            entity.addImages(request.imageUrls());
        }

        ClubRegistrationRequest saved = clubRegistrationRequestRepository.save(entity);
        applicationEventPublisher.publishEvent(ClubRegistrationRequestedEvent.from(saved));
    }

    public void requestInformationUpdate(Integer clubId, ClubInformationUpdateRequestDto request) {
        WebClub club = websiteQueryRepository.findClub(clubId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_CLUB));
        ClubInformationUpdateRequest entity = ClubInformationUpdateRequest.builder()
            .club(club)
            .universityName(request.universityName())
            .clubName(request.clubName())
            .clubCategory(request.clubCategory())
            .clubTopic(request.clubTopic())
            .clubEmoji(request.clubEmoji())
            .shortDescription(request.shortDescription())
            .fullIntroduction(request.fullIntroduction())
            .status(ClubInformationUpdateRequest.UpdateRequestStatus.PENDING)
            .build();

        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            entity.addImages(request.imageUrls());
        }

        ClubInformationUpdateRequest saved = clubInformationUpdateRequestRepository.save(entity);
        applicationEventPublisher.publishEvent(ClubInformationUpdateRequestedEvent.from(saved));
    }
}
