package gg.agit.konect.infrastructure.slack.service;

import static gg.agit.konect.infrastructure.slack.enums.SlackMessageTemplate.ADMIN_CHAT_RECEIVED;
import static gg.agit.konect.infrastructure.slack.enums.SlackMessageTemplate.CLUB_INFORMATION_UPDATE_REQUEST;
import static gg.agit.konect.infrastructure.slack.enums.SlackMessageTemplate.CLUB_REGISTRATION_REQUEST;
import static gg.agit.konect.infrastructure.slack.enums.SlackMessageTemplate.INQUIRY;
import static gg.agit.konect.infrastructure.slack.enums.SlackMessageTemplate.SHEET_SYNC_FAILED;
import static gg.agit.konect.infrastructure.slack.enums.SlackMessageTemplate.USER_REGISTER;
import static gg.agit.konect.infrastructure.slack.enums.SlackMessageTemplate.USER_WITHDRAWAL;

import java.util.List;

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

    public void notifyClubRegistrationRequest(
        Integer requestId,
        String universityName,
        String clubName,
        String category,
        String topic,
        String emoji,
        String description,
        String fullIntroduction,
        List<String> imageUrls
    ) {
        String message = CLUB_REGISTRATION_REQUEST.format(
            universityName,
            emoji,
            clubName,
            category,
            topic,
            emoji,
            description,
            fullIntroduction,
            formatImageUrls(imageUrls)
        );
        slackClient.sendMessage(message, slackProperties.webhooks().event());
    }

    public void notifyClubInformationUpdateRequest(
        Integer requestId,
        Integer clubId,
        String currentClubName,
        String requestedClubName,
        String category,
        String description,
        String imageUrl,
        String location,
        String fullIntroduction
    ) {
        String message = CLUB_INFORMATION_UPDATE_REQUEST.format(
            requestId,
            clubId,
            currentClubName,
            requestedClubName,
            category,
            description,
            imageUrl,
            location,
            fullIntroduction
        );
        slackClient.sendMessage(message, slackProperties.webhooks().event());
    }

    private String formatImageUrls(List<String> imageUrls) {
        if (imageUrls.isEmpty()) {
            return "없음";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < imageUrls.size(); i++) {
            builder.append(imageUrls.get(i));
            if (i < imageUrls.size() - 1) {
                builder.append(System.lineSeparator());
            }
        }
        return builder.toString();
    }
}
