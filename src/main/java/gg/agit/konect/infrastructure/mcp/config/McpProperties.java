package gg.agit.konect.infrastructure.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "mcp")
public record McpProperties(
    @NotBlank
    String url
) {

}
