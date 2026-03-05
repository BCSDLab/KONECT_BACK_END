package gg.agit.konect.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.TestPropertySource;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TestPropertySource(locations = "classpath:.env.test.properties", properties = {
    "spring.config.import=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.url=jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.javax.persistence.validation.mode=none",
    "spring.flyway.enabled=false",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "spring.security.enabled=false",
    "logging.ignored-url-patterns="
})
public @interface TestPropertyConfig {
}
