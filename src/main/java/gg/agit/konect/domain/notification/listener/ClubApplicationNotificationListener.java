package gg.agit.konect.domain.notification.listener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import gg.agit.konect.domain.club.event.ClubApplicationApprovedEvent;
import gg.agit.konect.domain.club.event.ClubApplicationSubmittedEvent;
import gg.agit.konect.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClubApplicationNotificationListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleClubApplicationApproved(ClubApplicationApprovedEvent event) {
        notificationService.sendClubApplicationApprovedNotification(
            event.receiverId(),
            event.clubId(),
            event.clubName()
        );
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleClubApplicationSubmitted(ClubApplicationSubmittedEvent event) {
        notificationService.sendClubApplicationSubmittedNotification(
            event.receiverId(),
            event.applicationId(),
            event.clubId(),
            event.clubName(),
            event.applicantName()
        );
    }
}
