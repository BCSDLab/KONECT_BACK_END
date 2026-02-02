package gg.agit.konect.infra.storage.s3;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3ClientConfig {

    @Bean
    public S3Client s3Client(S3StorageProperties s3StorageProperties) {
        return S3Client.builder()
            .region(Region.of(s3StorageProperties.region()))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build();
    }
}
