package gg.agit.konect.infrastructure.slack.listener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import gg.agit.konect.domain.club.event.ClubRegistrationRequestedEvent;
import gg.agit.konect.infrastructure.slack.service.SlackNotificationService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClubRegistrationRequestSlackListener {

    private final SlackNotificationService slackNotificationService;

    @Async("slackTaskExecutor")
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleClubRegistrationRequested(ClubRegistrationRequestedEvent event) {
        slackNotificationService.notifyClubRegistrationRequest(event);
    }
}
