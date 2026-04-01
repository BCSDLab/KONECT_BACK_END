package gg.agit.konect.integration.infrastructure.oauth;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import gg.agit.konect.infrastructure.oauth.GoogleDriveOAuthService;
import gg.agit.konect.support.IntegrationTestSupport;

class GoogleDriveOAuthControllerTest extends IntegrationTestSupport {

    @MockitoBean
    private GoogleDriveOAuthService googleDriveOAuthService;

    private static final Integer USER_ID = 100;
    private static final String AUTHORIZATION_URL =
        "https://accounts.google.com/o/oauth2/v2/auth?client_id=test-client&state=test-state";

    @BeforeEach
    void setUp() throws Exception {
        mockLoginUser(USER_ID);
    }

    @Nested
    @DisplayName("GET /auth/oauth/google/drive/authorize-url - Google Drive 권한 연결 URL 조회")
    class GetAuthorizationUrl {

        @Test
        @DisplayName("로그인 사용자의 authorize URL을 JSON으로 반환한다")
        void getAuthorizationUrl() throws Exception {
            given(googleDriveOAuthService.buildAuthorizationUrl(eq(USER_ID)))
                .willReturn(AUTHORIZATION_URL);

            performGet("/auth/oauth/google/drive/authorize-url")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl").value(AUTHORIZATION_URL));
        }
    }
}
