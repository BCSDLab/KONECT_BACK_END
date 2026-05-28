package gg.agit.konect.infrastructure.slack.listener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import gg.agit.konect.domain.club.event.ClubInformationUpdateRequestedEvent;
import gg.agit.konect.domain.club.event.ClubRegistrationRequestedEvent;
import gg.agit.konect.infrastructure.slack.service.SlackNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClubRegistrationRequestSlackListener {

    private final SlackNotificationService slackNotificationService;

    @Async("slackTaskExecutor")
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleClubRegistrationRequested(ClubRegistrationRequestedEvent event) {
        try {
            slackNotificationService.notifyClubRegistrationRequest(
                event.requestId(),
                event.universityName(),
                event.clubName(),
                event.category(),
                event.topic(),
                event.emoji(),
                event.description(),
                event.fullIntroduction(),
                event.imageUrls()
            );
        } catch (RuntimeException e) {
            log.warn("Failed to send club registration request Slack notification. requestId={}", event.requestId(), e);
        }
    }

    @Async("slackTaskExecutor")
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleClubInformationUpdateRequested(ClubInformationUpdateRequestedEvent event) {
        try {
            slackNotificationService.notifyClubInformationUpdateRequest(
                event.requestId(),
                event.clubId(),
                event.currentUniversityName(),
                event.requestedUniversityName(),
                event.currentClubName(),
                event.requestedClubName(),
                event.currentCategory(),
                event.requestedCategory(),
                event.currentTopic(),
                event.requestedTopic(),
                event.requestedEmoji(),
                event.currentDescription(),
                event.requestedDescription(),
                event.currentFullIntroduction(),
                event.requestedFullIntroduction(),
                event.currentImageUrl(),
                event.requestedImageUrls()
            );
        } catch (RuntimeException e) {
            log.warn(
                "Failed to send club information update request Slack notification. requestId={}",
                event.requestId(),
                e
            );
        }
    }
}
