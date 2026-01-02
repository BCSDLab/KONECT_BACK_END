package gg.agit.konect.infrastructure.slack.listener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import gg.agit.konect.domain.user.event.UserRegisterEvent;
import gg.agit.konect.domain.user.event.UserWithdrawEvent;
import gg.agit.konect.infrastructure.slack.service.SlackNotificationService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserSlackListener {

    private final SlackNotificationService slackNotificationService;

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleUserWithdraw(UserWithdrawEvent event) {
        slackNotificationService.notifyUserWithdraw(event.email());
    }

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleUserRegister(UserRegisterEvent event) {
        slackNotificationService.notifyUserRegister(event.email());
    }
}
