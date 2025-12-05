package gg.agit.konect.security.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import gg.agit.konect.security.oauth.service.GoogleOAuthServiceImpl;
import gg.agit.konect.security.oauth.service.SocialOAuthService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private Map<String, SocialOAuthService> oAuthServices;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, GoogleOAuthServiceImpl googleOAuthServiceImpl) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login/oauth2/code/**",
                    "/error"
                ).permitAll()
                .anyRequest().authenticated()
            ).oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(userRequest -> {
                        String registrationId = userRequest.getClientRegistration().getRegistrationId();
                        return oAuthServices.get(registrationId).loadUser(userRequest);
                    })
                )
            );

        return http.build();
    }
}
