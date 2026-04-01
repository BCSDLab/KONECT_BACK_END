package gg.agit.konect.infrastructure.slack.service;

import static gg.agit.konect.infrastructure.slack.enums.SlackMessageTemplate.ADMIN_CHAT_RECEIVED;
import static gg.agit.konect.infrastructure.slack.enums.SlackMessageTemplate.INQUIRY;
import static gg.agit.konect.infrastructure.slack.enums.SlackMessageTemplate.SHEET_SYNC_FAILED;
import static gg.agit.konect.infrastructure.slack.enums.SlackMessageTemplate.USER_REGISTER;
import static gg.agit.konect.infrastructure.slack.enums.SlackMessageTemplate.USER_WITHDRAWAL;

import org.springframework.stereotype.Service;

import gg.agit.konect.domain.club.event.SheetSyncFailedEvent;
import gg.agit.konect.infrastructure.slack.client.SlackClient;
import gg.agit.konect.infrastructure.slack.config.SlackProperties;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SlackNotificationService {

    private final SlackProperties slackProperties;
    private final SlackClient slackClient;

    public void notifyUserWithdraw(String email, String provider) {
        String message = USER_WITHDRAWAL.format(email, provider);
        slackClient.sendMessage(message, slackProperties.webhooks().event());
    }

    public void notifyUserRegister(String email, String provider) {
        String message = USER_REGISTER.format(email, provider);
        slackClient.sendMessage(message, slackProperties.webhooks().event());
    }

    public void notifyInquiry(String content) {
        String message = INQUIRY.format(content);
        slackClient.sendMessage(message, slackProperties.webhooks().event());
    }

    public void notifyAdminChatReceived(String senderName, String content) {
        String message = ADMIN_CHAT_RECEIVED.format(senderName, content);
        slackClient.sendMessage(message, slackProperties.webhooks().event());
    }

    public void notifySheetSyncFailed(SheetSyncFailedEvent event) {
        String message = SHEET_SYNC_FAILED.format(
            event.clubId(),
            event.spreadsheetId(),
            event.accessDenied() ? "ACCESS_DENIED" : "UNEXPECTED",
            event.occurredAt(),
            event.reason()
        );
        slackClient.sendMessage(message, slackProperties.webhooks().error());
    }
}
