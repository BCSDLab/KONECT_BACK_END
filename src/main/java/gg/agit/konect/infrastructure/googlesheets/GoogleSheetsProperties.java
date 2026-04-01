package gg.agit.konect.infrastructure.googlesheets;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "google.sheets")
public record GoogleSheetsProperties(
    String credentialsPath,
    String applicationName,
    @NotBlank String oauthClientId,
    @NotBlank String oauthClientSecret,
    @NotBlank String oauthCallbackBaseUrl
) {
}
