package gg.agit.konect.infrastructure.slack.listener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import gg.agit.konect.domain.user.event.UserWithdrawEvent;
import gg.agit.konect.infrastructure.slack.service.SlackNotificationService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserSlackListener {

    private final SlackNotificationService slackNotificationService;

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleUserWithdrawn(UserWithdrawEvent event) {
        slackNotificationService.notifyUserWithdrawal(event.email());
    }
}
