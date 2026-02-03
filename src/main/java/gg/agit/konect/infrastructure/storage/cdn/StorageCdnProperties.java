package gg.agit.konect.infrastructure.storage.cdn;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.cdn")
public record StorageCdnProperties(
    String baseUrl
) {

}
