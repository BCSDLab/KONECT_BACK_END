package gg.agit.konect.global.config;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springdoc.core.models.GroupedOpenApi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

    private final String serverUrl;

    public SwaggerConfig(
        @Value("${swagger.server-url}") String serverUrl
    ) {
        this.serverUrl = serverUrl;
    }

    @Bean
    public OpenAPI openAPI() {
        Server server = new Server();
        server.setUrl(serverUrl);

        return new OpenAPI()
            .openapi("3.1.0")
            .info(apiInfo())
            .addServersItem(server);
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("Public API")
            .pathsToMatch("/**")
            .pathsToExclude("/admin/**")
            .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("Admin API")
            .pathsToMatch("/admin/**")
            .build();
    }

    private Info apiInfo() {
        return new Info()
            .title("KONECT API")
            .description("KONECT API 문서입니다.")
            .version(LocalDate.now().toString());
    }
}
