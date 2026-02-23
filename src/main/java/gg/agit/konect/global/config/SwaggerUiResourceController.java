package gg.agit.konect.global.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SwaggerUiResourceController {

    private static final String SWAGGER_INITIALIZER_PATH = "static/swagger-ui/swagger-initializer.js";

    @GetMapping(value = "/swagger-ui/swagger-initializer.js", produces = "application/javascript")
    public ResponseEntity<Resource> swaggerInitializer() {
        Resource resource = new ClassPathResource(SWAGGER_INITIALIZER_PATH);

        return ResponseEntity.ok()
            .contentType(MediaType.valueOf("application/javascript"))
            .body(resource);
    }
}
