package gg.agit.konect.support;

import static org.mockito.Mockito.mock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public OAuth2AuthorizedClientService oAuth2AuthorizedClientService() {
        return mock(OAuth2AuthorizedClientService.class);
    }

    @Bean
    @Primary
    public ClientRegistrationRepository clientRegistrationRepository() {
        return mock(ClientRegistrationRepository.class);
    }
}
