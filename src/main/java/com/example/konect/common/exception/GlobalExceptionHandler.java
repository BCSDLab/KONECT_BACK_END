package com.example.konect.common.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.konect.infrastructure.slack.client.SlackClient;
import com.example.konect.infrastructure.slack.model.SlackNotification;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final SlackClient slackClient;

    @Value("${slack.webhook.url}")
    private String slackUrl;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(HttpServletRequest request, Exception e) {
        sendSlackNotification(request, e);

        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "An internal server error occurred.");
        return ResponseEntity.internalServerError().body(response);
    }

    private void sendSlackNotification(HttpServletRequest request, Exception e) {
        StackTraceElement origin = e.getStackTrace()[0];

        String errorLocation = String.format(
            "%s:%d",
            origin.getFileName(),
            origin.getLineNumber()
        );

        String message = String.format(
            """
                🚨 서버에서 에러가 발생했습니다! 🚨
                URI: `%s %s`
                Location: `%s`
                Exception: `%s`
                Message: `%s`
            """,
            request.getMethod(),
            request.getRequestURI(),
            errorLocation,
            e.getClass().getSimpleName(),
            e.getMessage()
        );

        SlackNotification slackNotification = SlackNotification.builder()
            .slackUrl(slackUrl)
            .text(message)
            .build();

        slackClient.sendMessage(slackNotification);
    }
}
