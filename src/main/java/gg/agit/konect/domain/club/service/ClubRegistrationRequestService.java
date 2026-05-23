package gg.agit.konect.domain.club.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.dto.ClubRegistrationRequestDto;
import gg.agit.konect.domain.club.model.ClubRegistrationRequest;
import gg.agit.konect.domain.club.repository.ClubRegistrationRequestRepository;
import gg.agit.konect.infrastructure.slack.service.SlackNotificationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ClubRegistrationRequestService {

    private final ClubRegistrationRequestRepository clubRegistrationRequestRepository;
    private final SlackNotificationService slackNotificationService;

    public void register(ClubRegistrationRequestDto request) {
        ClubRegistrationRequest entity = ClubRegistrationRequest.builder()
            .universityName(request.universityName())
            .clubName(request.clubName())
            .clubCategory(request.clubCategory())
            .clubTopic(request.clubTopic())
            .clubEmoji(request.clubEmoji())
            .shortDescription(request.shortDescription())
            .fullIntroduction(request.fullIntroduction())
            .imageUrls(request.imageUrls())
            .status(ClubRegistrationRequest.RegistrationStatus.PENDING)
            .build();

        ClubRegistrationRequest saved = clubRegistrationRequestRepository.save(entity);

        slackNotificationService.notifyClubRegistrationRequest(
            saved.getId(),
            request.universityName(),
            request.clubName(),
            request.clubCategory().getDescription(),
            request.clubTopic(),
            request.clubEmoji(),
            request.shortDescription(),
            request.imageUrls() != null ? request.imageUrls().size() : 0
        );
    }
}
