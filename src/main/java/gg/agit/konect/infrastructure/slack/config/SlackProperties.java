package gg.agit.konect.infrastructure.slack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slack")
public record SlackProperties(
    Webhooks webhooks,
    String signingSecret,
    String botToken
) {
    public record Webhooks(
        String error,
        String event
    ) {

    }
}
