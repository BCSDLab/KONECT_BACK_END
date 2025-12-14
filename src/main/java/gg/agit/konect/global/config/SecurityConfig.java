package gg.agit.konect.global.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import gg.agit.konect.security.handler.OAuth2LoginSuccessHandler;
import gg.agit.konect.security.oauth.service.SocialOAuthService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private Map<String, SocialOAuthService> oAuthServices;

    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // TODO 테스트 이후 접근 가능 경로 막아두기
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(401);
                })
            ).oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(userRequest -> {
                        String registrationId = userRequest.getClientRegistration().getRegistrationId();
                        return oAuthServices.get(registrationId).loadUser(userRequest);
                    })
                ).successHandler(oAuth2LoginSuccessHandler)
            );

        return http.build();
    }
}
