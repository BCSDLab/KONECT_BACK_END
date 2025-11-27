package com.example.konect.infrastructure.slack.model;

import lombok.Builder;
import lombok.Getter;

@Getter
public class SlackNotification {

    public static final String COLOR_DANGER = "danger";

    private final String title;
    private final String content;

    @Builder
    private SlackNotification(
        String title,
        String text
    ) {
        this.title = title;
        this.content = text;
    }
}
