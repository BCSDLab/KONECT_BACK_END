package com.example.konect.infrastructure.slack.model;

import lombok.Builder;
import lombok.Getter;

@Getter
public class SlackNotification {

    public static final String COLOR_GOOD = "good";

    private final String content;

    @Builder
    private SlackNotification(
        String text
    ) {
        this.content = text;
    }
}
