package gg.agit.konect.infrastructure.slack.listener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import gg.agit.konect.domain.user.event.UserRegisteredEvent;
import gg.agit.konect.domain.user.event.UserWithdrawnEvent;
import gg.agit.konect.infrastructure.slack.service.SlackNotificationService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserSlackListener {

    private final SlackNotificationService slackNotificationService;

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleUserWithdrawn(UserWithdrawnEvent event) {
        slackNotificationService.notifyUserWithdraw(event.email(), event.provider());
    }

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleUserRegistered(UserRegisteredEvent event) {
        slackNotificationService.notifyUserRegister(event.email(), event.provider());
    }
}
