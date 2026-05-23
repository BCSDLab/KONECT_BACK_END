package gg.agit.konect.domain.club.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.dto.ClubRegistrationRequestDto;
import gg.agit.konect.domain.club.event.ClubRegistrationRequestedEvent;
import gg.agit.konect.domain.club.model.ClubRegistrationRequest;
import gg.agit.konect.domain.club.repository.ClubRegistrationRequestRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ClubRegistrationRequestService {

    private final ClubRegistrationRequestRepository clubRegistrationRequestRepository;
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
}
