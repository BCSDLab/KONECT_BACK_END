package gg.agit.konect.global.config;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    private static final Integer CONNECT_TIMEOUT = 5000;
    private static final Integer READ_TIMEOUT = 5000;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
            .requestFactory(() -> {
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(CONNECT_TIMEOUT);
                factory.setReadTimeout(READ_TIMEOUT);
                return new BufferingClientHttpRequestFactory(factory);
            })
            .additionalMessageConverters(new StringHttpMessageConverter(UTF_8))
            .build();
    }
}
