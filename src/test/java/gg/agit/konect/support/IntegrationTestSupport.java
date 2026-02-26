package gg.agit.konect.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestJpaConfig.class})
@EnableAutoConfiguration(exclude = {
    OAuth2ClientAutoConfiguration.class
})
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
public abstract class IntegrationTestSupport {

    @Autowired
    protected EntityManager entityManager;

    protected <T> T persist(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }

    protected void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
