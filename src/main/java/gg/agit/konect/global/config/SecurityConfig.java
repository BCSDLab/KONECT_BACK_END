package gg.agit.konect.global.config;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import gg.agit.konect.global.auth.handler.OAuth2LoginSuccessHandler;
import gg.agit.konect.global.auth.oauth.SocialOAuthService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private Map<String, SocialOAuthService> oAuthServices;

    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(SecurityPaths.DENY_PATHS).denyAll()
                .requestMatchers(SecurityPaths.PUBLIC_PATHS).permitAll()
                .anyRequest().permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(UNAUTHORIZED.value());
                })
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(userRequest -> {
                        String registrationId = userRequest.getClientRegistration().getRegistrationId();
                        return oAuthServices.get(registrationId).loadUser(userRequest);
                    })
                )
                .successHandler(oAuth2LoginSuccessHandler)
            );

        return http.build();
    }

    @Bean
    public CookieSameSiteSupplier applicationCookieSameSiteSupplier() {
        return CookieSameSiteSupplier.ofNone();
    }
}
