package gg.agit.konect.unit.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.cors.CorsConfiguration;

import gg.agit.konect.global.auth.web.AuthorizationInterceptor;
import gg.agit.konect.global.auth.web.LoginCheckInterceptor;
import gg.agit.konect.global.auth.web.LoginUserArgumentResolver;
import gg.agit.konect.global.config.CorsProperties;
import gg.agit.konect.global.config.WebConfig;

class WebConfigTest {

    @Test
    @DisplayName("CORS 응답은 request id 헤더를 브라우저에 노출한다")
    void exposesRequestIdHeader() throws Exception {
        // given
        WebConfig webConfig = new WebConfig(
            new CorsProperties(List.of("http://localhost:3000")),
            org.mockito.Mockito.mock(LoginCheckInterceptor.class),
            org.mockito.Mockito.mock(AuthorizationInterceptor.class),
            org.mockito.Mockito.mock(LoginUserArgumentResolver.class)
        );
        CorsRegistry registry = new CorsRegistry();

        // when
        webConfig.addCorsMappings(registry);

        // then
        CorsConfiguration corsConfiguration = firstCorsConfiguration(registry);
        assertThat(corsConfiguration.getExposedHeaders())
            .contains("Authorization", "X-Request-ID");
    }

    @SuppressWarnings("unchecked")
    private CorsConfiguration firstCorsConfiguration(CorsRegistry registry) throws Exception {
        Field registrationsField = CorsRegistry.class.getDeclaredField("registrations");
        registrationsField.setAccessible(true);
        List<CorsRegistration> registrations = (List<CorsRegistration>)registrationsField.get(registry);

        Field configField = CorsRegistration.class.getDeclaredField("config");
        configField.setAccessible(true);
        return (CorsConfiguration)configField.get(registrations.getFirst());
    }
}
