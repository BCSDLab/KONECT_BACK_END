package gg.agit.konect.domain.club.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.dto.ClubRegistrationRequest;
import gg.agit.konect.domain.club.event.ClubRegistrationRequestedEvent;
import gg.agit.konect.domain.club.model.ClubRegistrationRequestEntity;
import gg.agit.konect.domain.club.repository.ClubRegistrationRequestRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClubRegistrationRequestService {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ClubRegistrationRequestRepository clubRegistrationRequestRepository;

    @Transactional
    public void submitClubRegistrationRequest(ClubRegistrationRequest request) {
        ClubRegistrationRequestEntity savedRequest =
            clubRegistrationRequestRepository.save(ClubRegistrationRequestEntity.from(request));
        applicationEventPublisher.publishEvent(ClubRegistrationRequestedEvent.from(savedRequest));
    }
}
