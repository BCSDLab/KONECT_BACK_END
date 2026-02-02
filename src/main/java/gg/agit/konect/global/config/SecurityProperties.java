package gg.agit.konect.global.config;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class SecurityProperties {

    private Set<String> allowedRedirectOrigins;
}
