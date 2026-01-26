package gg.agit.konect.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.cdn")
public record StorageCdnProperties(
    String baseUrl
) {

}
