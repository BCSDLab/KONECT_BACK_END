package gg.agit.konect.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestJpaConfig.class, TestClaudeConfig.class})
@EnableAutoConfiguration(exclude = {
    OAuth2ClientAutoConfiguration.class
})
@TestPropertyConfig
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
