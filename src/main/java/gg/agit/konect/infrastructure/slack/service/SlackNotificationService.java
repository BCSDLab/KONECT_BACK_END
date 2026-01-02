package gg.agit.konect.infrastructure.slack.service;

import static gg.agit.konect.infrastructure.slack.enums.SlackMessageTemplate.USER_WITHDRAWAL;

import org.springframework.stereotype.Service;

import gg.agit.konect.infrastructure.slack.client.SlackClient;
import gg.agit.konect.infrastructure.slack.config.SlackProperties;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SlackNotificationService {

    private final SlackProperties slackProperties;
    private final SlackClient slackClient;

    public void notifyUserWithdrawal(String email) {
        String message = USER_WITHDRAWAL.format(email);
        slackClient.sendMessage(message, slackProperties.webhooks().event());
    }
}
