package gg.agit.konect.infrastructure.claude.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "claude")
public record ClaudeProperties(
    @NotBlank String apiKey,
    @NotBlank String model
) { }
