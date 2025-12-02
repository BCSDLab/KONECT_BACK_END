package gg.agit.konect.global.logging;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logging")
public record LoggingProperties(
    List<String> ignoredUrlPatterns
) {

}
