package gg.agit.konect.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import jakarta.persistence.EntityManager;

@Execution(ExecutionMode.SAME_THREAD)
@SpringBootTest
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestJpaConfig.class, TestClaudeConfig.class})
@TestPropertyConfig
@Transactional
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
