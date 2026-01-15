package gg.agit.konect.global.config;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;

import gg.agit.konect.global.auth.filter.OAuth2RedirectUriSaveFilter;
import gg.agit.konect.global.auth.handler.OAuth2LoginSuccessHandler;
import gg.agit.konect.global.auth.oauth.AppleOAuthServiceImpl;
import gg.agit.konect.global.auth.oauth.AppleClientSecretProvider;
import gg.agit.konect.global.auth.oauth.CustomRequestEntityConverter;
import gg.agit.konect.global.auth.oauth.SocialOAuthService;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Autowired
    private Map<String, SocialOAuthService> oAuthServices;

    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Autowired
    private OAuth2RedirectUriSaveFilter redirectUriSaveFilter;

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> appleAccessTokenResponseClient,
        AppleOAuthServiceImpl appleOAuthService
    ) throws Exception {
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
                .tokenEndpoint(token -> token
                    .accessTokenResponseClient(appleAccessTokenResponseClient)
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(userRequest -> {
                        String registrationId = userRequest.getClientRegistration().getRegistrationId();
                        return oAuthServices.get(registrationId).loadUser(userRequest);
                    })
                    .oidcUserService(appleOAuthService)
                )
                .successHandler(oAuth2LoginSuccessHandler)
                .failureHandler((request, response, exception) -> {
                    response.sendRedirect(frontendBaseUrl);
                })
            );

        http.addFilterBefore(redirectUriSaveFilter, OAuth2AuthorizationRequestRedirectFilter.class);

        return http.build();
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> appleAccessTokenResponseClient(
        AppleClientSecretProvider appleClientSecretProvider
    ) {
        RestClientAuthorizationCodeTokenResponseClient client = new RestClientAuthorizationCodeTokenResponseClient();
        client.setParametersConverter(new CustomRequestEntityConverter(appleClientSecretProvider));
        return client;
    }
}
