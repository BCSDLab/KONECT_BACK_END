package gg.agit.konect.infra.storage.cdn;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.cdn")
public record StorageCdnProperties(
    String baseUrl
) {

}
