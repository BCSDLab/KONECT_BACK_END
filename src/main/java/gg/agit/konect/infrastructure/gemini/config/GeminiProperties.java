package gg.agit.konect.infrastructure.gemini.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
    String projectId,
    String location,
    String model
) {

}
