package gg.agit.konect.global.auth.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.Cookie;

class AuthCookieServiceTest {

    private AuthCookieService authCookieService;

    @BeforeEach
    void setUp() {
        authCookieService = new AuthCookieService();
        ReflectionTestUtils.setField(authCookieService, "cookieDomain", "konect.test");
    }

    @Test
    @DisplayName("setRefreshToken은 HTTPS 요청에 Secure/SameSite=None 쿠키를 설정한다")
    void setRefreshTokenUsesSecureCookieForHttpsRequest() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        authCookieService.setRefreshToken(request, response, "refresh-token", Duration.ofMinutes(5));

        // then
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie)
            .contains("refresh_token=refresh-token")
            .contains("Max-Age=300")
            .contains("Domain=konect.test")
            .contains("Path=/")
            .contains("Secure")
            .contains("HttpOnly")
            .contains("SameSite=None");
    }

    @Test
    @DisplayName("setRefreshToken은 X-Forwarded-Proto=https도 보안 요청으로 취급한다")
    void setRefreshTokenTreatsForwardedHttpsAsSecure() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        authCookieService.setRefreshToken(request, response, "refresh-token", Duration.ofSeconds(30));

        // then
        assertThat(response.getHeader("Set-Cookie"))
            .contains("Secure")
            .contains("SameSite=None");
    }

    @Test
    @DisplayName("clearSignupToken은 만료된 빈 쿠키를 내려준다")
    void clearSignupTokenSetsExpiredEmptyCookie() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        authCookieService.clearSignupToken(request, response);

        // then
        assertThat(response.getHeader("Set-Cookie"))
            .contains("signup_token=")
            .contains("Max-Age=0")
            .contains("Path=/")
            .contains("HttpOnly");
    }

    @Test
    @DisplayName("getCookieValue는 대상 쿠키를 찾고 없으면 null을 반환한다")
    void getCookieValueReturnsMatchingCookieValueOrNull() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
            new Cookie("other", "123"),
            new Cookie(AuthCookieService.REFRESH_TOKEN_COOKIE, "refresh-value")
        );

        // when & then
        assertThat(authCookieService.getCookieValue(request, AuthCookieService.REFRESH_TOKEN_COOKIE))
            .isEqualTo("refresh-value");
        assertThat(authCookieService.getCookieValue(request, AuthCookieService.SIGNUP_TOKEN_COOKIE)).isNull();
        assertThat(authCookieService.getCookieValue(new MockHttpServletRequest(), "missing")).isNull();
    }
}
