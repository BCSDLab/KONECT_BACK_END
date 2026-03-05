package gg.agit.konect;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import gg.agit.konect.support.TestGeminiConfig;
import gg.agit.konect.support.TestSecurityConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestGeminiConfig.class, TestSecurityConfig.class})
@TestPropertySource(locations = "classpath:.env.test.properties")
class KonectApplicationTests {

    @Test
    void contextLoads() {
    }

}


