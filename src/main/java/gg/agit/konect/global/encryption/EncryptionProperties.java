package gg.agit.konect.global.encryption;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@Component
@ConfigurationProperties(prefix = "app.encryption")
public class EncryptionProperties {

    private String chatKey;
    private boolean migrationEnabled;
}
