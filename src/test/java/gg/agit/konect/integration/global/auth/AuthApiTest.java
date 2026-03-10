package gg.agit.konect.integration.global.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.jayway.jsonpath.JsonPath;

import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.service.SignupTokenService;
import gg.agit.konect.global.auth.jwt.JwtProvider;
import gg.agit.konect.global.auth.oauth.NativeSessionBridgeService;
import gg.agit.konect.global.auth.oauth.OAuthLoginHelper;
import gg.agit.konect.global.auth.oauth.VerifiedOAuthUser;
import gg.agit.konect.infrastructure.oauth.AppleTokenVerifier;
import gg.agit.konect.infrastructure.oauth.GoogleTokenVerifier;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class AuthApiTest extends IntegrationTestSupport {

    private static final int BRIDGE_USER_ID = 2024001003;
    private static final Duration SIGNUP_TOKEN_TTL = Duration.ofMinutes(10);

    @MockitoBean
    private GoogleTokenVerifier googleTokenVerifier;

    @MockitoBean
    private AppleTokenVerifier appleTokenVerifier;

    @MockitoBean
    private OAuthLoginHelper oauthLoginHelper;

    @MockitoBean
    private SignupTokenService signupTokenService;

    @MockitoBean
    private NativeSessionBridgeService nativeSessionBridgeService;

    @Autowired
    private JwtProvider jwtProvider;

    private University university;

    @BeforeEach
    void setUp() {
        university = persist(UniversityFixture.create());
        given(googleTokenVerifier.provider()).willReturn(Provider.GOOGLE);
        given(appleTokenVerifier.provider()).willReturn(Provider.APPLE);
        given(jwtProvider.createToken(anyInt())).willReturn("issued-access-token");
        given(signupTokenService.signupTtl()).willReturn(SIGNUP_TOKEN_TTL);
    }

    @Nested
    class OAuthTokenLogin {

        @Test
        void 기존_구글_회원은_로그인하고_리프레시_쿠키를_발급한다() throws Exception {
            // given
            User existingUser = persist(UserFixture.createUser(university, "구글회원", "2024001001"));
            clearPersistenceContext();

            given(googleTokenVerifier.verify(any())).willReturn(
                new VerifiedOAuthUser("google-provider-id", existingUser.getEmail(), "구글회원")
            );
            given(
                oauthLoginHelper.findUserByProvider(
                    Provider.GOOGLE,
                    existingUser.getEmail(),
                    "google-provider-id"
                )
            )
                .willReturn(Optional.of(existingUser));
            given(oauthLoginHelper.resolveSafeRedirect("http://localhost:3000/feed"))
                .willReturn("http://localhost:3000/feed");
            given(oauthLoginHelper.isAppleOauthCallback("http://localhost:3000/feed")).willReturn(false);

            // when, then
            performPost("/auth/oauth/token", oauthLoginRequest("GOOGLE", "google-access-token", null,
                "http://localhost:3000/feed", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("issued-access-token"))
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.redirectUri").value("http://localhost:3000/feed"))
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    String refreshToken = JsonPath.read(responseBody, "$.refreshToken");
                    List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
                    assertThat(setCookies).anyMatch(cookie -> cookie.contains("refresh_token=" + refreshToken));
                    assertThat(setCookies).anyMatch(
                        cookie -> cookie.contains("signup_token=") && cookie.contains("Max-Age=0")
                    );
                    assertThat(setCookies).anyMatch(cookie -> cookie.contains("HttpOnly") && cookie.contains("Path=/"));
                });
        }

        @Test
        void 신규_애플_회원은_요청_이름으로_회원가입_토큰을_발급한다() throws Exception {
            // given
            given(appleTokenVerifier.verify(any())).willReturn(
                new VerifiedOAuthUser("apple-provider-id", "new-apple@koreatech.ac.kr", "")
            );
            given(
                oauthLoginHelper.findUserByProvider(
                    Provider.APPLE,
                    "new-apple@koreatech.ac.kr",
                    "apple-provider-id"
                )
            )
                .willReturn(Optional.empty());
            given(
                signupTokenService.issue(
                    "new-apple@koreatech.ac.kr",
                    Provider.APPLE,
                    "apple-provider-id",
                    "앱등이"
                )
            )
                .willReturn("signup-token-apple");

            // when, then
            performPost("/auth/oauth/token", oauthLoginRequest("APPLE", null, "apple-id-token",
                "konect://oauth/callback", "앱등이"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signupToken").value("signup-token-apple"))
                .andExpect(jsonPath("$.name").value("앱등이"))
                .andExpect(result -> {
                    List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
                    assertThat(setCookies).anyMatch(
                        cookie -> cookie.contains("signup_token=signup-token-apple")
                    );
                    assertThat(setCookies).noneMatch(
                        cookie -> cookie.contains("refresh_token=") && !cookie.contains("Max-Age=0")
                    );
                    assertThat(setCookies).anyMatch(cookie -> cookie.contains("HttpOnly") && cookie.contains("Path=/"));
                });
        }

        @Test
        void 기존_애플_회원이_네이티브_콜백으로_로그인하면_브릿지_토큰을_내려준다() throws Exception {
            // given
            User existingUser = persist(UserFixture.createUser(university, "애플회원", "2024001002"));
            clearPersistenceContext();

            given(appleTokenVerifier.verify(any())).willReturn(
                new VerifiedOAuthUser("apple-existing-id", existingUser.getEmail(), "애플회원")
            );
            given(
                oauthLoginHelper.findUserByProvider(
                    Provider.APPLE,
                    existingUser.getEmail(),
                    "apple-existing-id"
                )
            )
                .willReturn(Optional.of(existingUser));
            given(oauthLoginHelper.resolveSafeRedirect("konect://oauth/callback"))
                .willReturn("konect://oauth/callback");
            given(oauthLoginHelper.isAppleOauthCallback("konect://oauth/callback")).willReturn(true);
            given(nativeSessionBridgeService.issue(existingUser.getId())).willReturn("bridge-token-123");
            given(oauthLoginHelper.appendBridgeToken("konect://oauth/callback", "bridge-token-123"))
                .willReturn("konect://oauth/callback?bridge_token=bridge-token-123");

            // when, then
            performPost("/auth/oauth/token", oauthLoginRequest("APPLE", null, "apple-id-token",
                "konect://oauth/callback", "애플회원"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("issued-access-token"))
                .andExpect(
                    jsonPath("$.redirectUri").value("konect://oauth/callback?bridge_token=bridge-token-123")
                )
                .andExpect(result -> {
                    assertThat(result.getResponse().getContentAsString()).doesNotContain("\"refreshToken\":\"");
                    List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
                    assertThat(setCookies).anyMatch(
                        cookie -> cookie.contains("refresh_token=") && cookie.contains("Max-Age=0")
                    );
                    assertThat(setCookies).anyMatch(
                        cookie -> cookie.contains("signup_token=") && cookie.contains("Max-Age=0")
                    );
                    assertThat(setCookies).anyMatch(cookie -> cookie.contains("HttpOnly") && cookie.contains("Path=/"));
                });
        }

        @Test
        void 지원하지_않는_프로바이더면_400_에러_코드를_반환한다() throws Exception {
            // given, when, then
            performPost("/auth/oauth/token", oauthLoginRequest("LINE", "unused-access-token", null,
                "http://localhost:3000/feed", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_PROVIDER"));
        }
    }

    @Nested
    class NativeSessionBridge {

        @Test
        void 유효한_브릿지_토큰이면_리프레시_쿠키를_발급한다() throws Exception {
            // given
            given(nativeSessionBridgeService.consume("bridge-token-success"))
                .willReturn(Optional.of(BRIDGE_USER_ID));

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("bridge_token", "bridge-token-success");

            // when, then
            performGet("/native/session/bridge", params)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(result -> {
                    List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
                    assertThat(setCookies).anyMatch(cookie -> cookie.contains("refresh_token="));
                    assertThat(setCookies).anyMatch(
                        cookie -> cookie.contains("signup_token=") && cookie.contains("Max-Age=0")
                    );
                    assertThat(result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL))
                        .isEqualTo("no-store, no-cache, must-revalidate");
                    assertThat(setCookies).anyMatch(cookie -> cookie.contains("HttpOnly") && cookie.contains("Path=/"));
                });
        }

        @Test
        void 유효하지_않은_브릿지_토큰이면_401을_반환하고_쿠키를_발급하지_않는다() throws Exception {
            // given
            given(nativeSessionBridgeService.consume(eq("bridge-token-invalid"))).willReturn(Optional.empty());

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("bridge_token", "bridge-token-invalid");

            // when, then
            performGet("/native/session/bridge", params)
                .andExpect(status().isUnauthorized())
                .andExpect(
                    result -> {
                        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
                        assertThat(result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL))
                            .isEqualTo("no-store, no-cache, must-revalidate");
                    }
                );
        }

        @Test
        void 브릿지_토큰이_없으면_401을_반환하고_쿠키를_발급하지_않는다() throws Exception {
            // given, when, then
            performGet("/native/session/bridge")
                .andExpect(status().isUnauthorized())
                .andExpect(
                    result -> {
                        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
                        assertThat(result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL))
                            .isEqualTo("no-store, no-cache, must-revalidate");
                    }
                );
        }
    }

    private Map<String, Object> oauthLoginRequest(
        String provider,
        String accessToken,
        String idToken,
        String redirectUri,
        String name
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("provider", provider);
        body.put("accessToken", accessToken);
        body.put("idToken", idToken);
        body.put("redirectUri", redirectUri);
        body.put("name", name);
        return body;
    }
}
