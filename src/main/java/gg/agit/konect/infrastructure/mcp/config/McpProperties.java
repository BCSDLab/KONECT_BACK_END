package gg.agit.konect.infrastructure.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp")
public record McpProperties(
    String url
) {

}
