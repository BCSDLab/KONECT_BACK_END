package gg.agit.konect.infrastructure.slack.listener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import gg.agit.konect.domain.club.event.SheetSyncFailedEvent;
import gg.agit.konect.infrastructure.slack.service.SlackNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SheetSyncSlackListener {

    private final SlackNotificationService slackNotificationService;

    @TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)
    public void handleSheetSyncFailed(SheetSyncFailedEvent event) {
        try {
            log.warn(
                "Handling sheet sync failure event. occurredAt={}, clubId={}, spreadsheetId={}, "
                    + "accessDenied={}, reason={}",
                event.occurredAt(),
                event.clubId(),
                event.spreadsheetId(),
                event.accessDenied(),
                event.reason()
            );
            slackNotificationService.notifySheetSyncFailed(event);
        } catch (Exception e) {
            log.error(
                "Failed to handle sheet sync failure event. clubId={}, spreadsheetId={}",
                event.clubId(),
                event.spreadsheetId(),
                e
            );
        }
    }
}
