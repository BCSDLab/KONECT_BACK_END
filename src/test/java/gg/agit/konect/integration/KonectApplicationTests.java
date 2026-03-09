package gg.agit.konect.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import gg.agit.konect.support.TestClaudeConfig;
import gg.agit.konect.support.TestSecurityConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestClaudeConfig.class, TestSecurityConfig.class})
@TestPropertySource(locations = "classpath:.env.test.properties")
class KonectApplicationTests {

    @Test
    void contextLoads() {
    }

}

