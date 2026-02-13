package gg.agit.konect.infrastructure.slack.listener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import gg.agit.konect.domain.chat.event.AdminChatReceivedEvent;
import gg.agit.konect.infrastructure.slack.service.SlackNotificationService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatSlackListener {

    private final SlackNotificationService slackNotificationService;

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleAdminChatReceived(AdminChatReceivedEvent event) {
        slackNotificationService.notifyAdminChatReceived(event.senderName(), event.content());
    }
}
