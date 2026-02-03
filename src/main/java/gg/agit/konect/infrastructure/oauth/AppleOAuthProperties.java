package gg.agit.konect.infrastructure.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.apple.oauth")
public class AppleOAuthProperties {

    private String teamId;
    private String clientId;
    private String keyId;
    private String privateKeyPath;
    private int tokenValidityDays;
    private int refreshBeforeDays;
}
