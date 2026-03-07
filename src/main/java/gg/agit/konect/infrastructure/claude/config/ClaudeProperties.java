package gg.agit.konect.infrastructure.claude.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "claude")
public record ClaudeProperties(
    @NotBlank String apiKey,
    @NotBlank String model
) { }
