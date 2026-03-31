package gg.agit.konect.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;
import com.google.auth.oauth2.GoogleCredentials;

import gg.agit.konect.support.TestClaudeConfig;
import gg.agit.konect.support.TestMcpConfig;
import gg.agit.konect.support.TestSecurityConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestClaudeConfig.class, TestMcpConfig.class, TestSecurityConfig.class})
@TestPropertySource(locations = "classpath:.env.test.properties")
class KonectApplicationTests {

    @MockitoBean
    private GoogleCredentials googleCredentials;

    @MockitoBean
    private Sheets googleSheetsService;

    @MockitoBean
    private Drive googleDriveService;

    @Test
    void contextLoads() {
    }

}
