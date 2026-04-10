package gg.agit.konect.unit.infrastructure.slack.listener;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.club.event.SheetSyncFailedEvent;
import gg.agit.konect.infrastructure.slack.listener.SheetSyncSlackListener;
import gg.agit.konect.infrastructure.slack.service.SlackNotificationService;
import gg.agit.konect.support.ServiceTestSupport;

class SheetSyncSlackListenerTest extends ServiceTestSupport {

    @Mock
    private SlackNotificationService slackNotificationService;

    @InjectMocks
    private SheetSyncSlackListener sheetSyncSlackListener;

    @Test
    @DisplayName("delegates sheet sync failure events to Slack notification service")
    void handleSheetSyncFailedDelegatesToSlackService() {
        SheetSyncFailedEvent event = SheetSyncFailedEvent.accessDenied(1, "spreadsheet-id", "access denied");

        sheetSyncSlackListener.handleSheetSyncFailed(event);

        verify(slackNotificationService).notifySheetSyncFailed(event);
    }

    @Test
    @DisplayName("swallows listener exceptions so event publishing flow is not broken")
    void handleSheetSyncFailedSwallowsExceptions() {
        SheetSyncFailedEvent event = SheetSyncFailedEvent.unexpected(1, "spreadsheet-id", "boom");
        doThrow(new RuntimeException("slack error"))
            .when(slackNotificationService)
            .notifySheetSyncFailed(event);

        sheetSyncSlackListener.handleSheetSyncFailed(event);

        verify(slackNotificationService).notifySheetSyncFailed(event);
    }
}
