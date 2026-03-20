package gg.agit.konect.infrastructure.googlesheets;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.sheets")
public record GoogleSheetsProperties(
    String credentialsPath,
    String applicationName
) {
}
