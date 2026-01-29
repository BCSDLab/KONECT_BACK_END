package gg.agit.konect.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.s3")
public record S3StorageProperties(
    String bucket,
    String region,
    String keyPrefix,
    Long maxUploadBytes
) {

}
