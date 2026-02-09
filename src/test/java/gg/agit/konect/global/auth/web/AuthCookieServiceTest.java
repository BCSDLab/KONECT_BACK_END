package gg.agit.konect.global.auth.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("AuthCookieService 단위 테스트")
class AuthCookieServiceTest {

    private static final String TOKEN = "token-value";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(10);

    private final AuthCookieService authCookieService = new AuthCookieService();

    @Nested
    @DisplayName("setRefreshToken 테스트")
    class SetRefreshTokenTests {

        @Test
        @DisplayName("HTTPS 요청이면 Secure, SameSite=None 쿠키를 설정한다")
        void setRefreshTokenWithSecureRequestSetsSecureCookie() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setSecure(true);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            authCookieService.setRefreshToken(request, response, TOKEN, TOKEN_TTL);

            // Then
            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie).contains("refresh_token=" + TOKEN);
            assertThat(setCookie).contains("Secure");
            assertThat(setCookie).contains("SameSite=None");
            assertThat(setCookie).contains("HttpOnly");
        }

        @Test
        @DisplayName("HTTP 요청이면 Secure 없이 쿠키를 설정한다")
        void setRefreshTokenWithInsecureRequestSetsInsecureCookie() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setSecure(false);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            authCookieService.setRefreshToken(request, response, TOKEN, TOKEN_TTL);

            // Then
            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie).contains("refresh_token=" + TOKEN);
            assertThat(setCookie).doesNotContain("Secure");
            assertThat(setCookie).doesNotContain("SameSite=None");
        }

        @Test
        @DisplayName("cookieDomain이 설정되면 Domain 속성을 포함한다")
        void setRefreshTokenWithDomainIncludesDomainAttribute() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setSecure(true);
            MockHttpServletResponse response = new MockHttpServletResponse();
            ReflectionTestUtils.setField(authCookieService, "cookieDomain", "example.com");

            // When
            authCookieService.setRefreshToken(request, response, TOKEN, TOKEN_TTL);

            // Then
            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie).contains("Domain=example.com");
        }

        @Test
        @DisplayName("X-Forwarded-Proto가 https면 Secure 쿠키를 설정한다")
        void setRefreshTokenWithForwardedProtoSetsSecureCookie() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setSecure(false);
            request.addHeader("X-Forwarded-Proto", "https");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            authCookieService.setRefreshToken(request, response, TOKEN, TOKEN_TTL);

            // Then
            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie).contains("Secure");
            assertThat(setCookie).contains("SameSite=None");
        }
    }

    @Nested
    @DisplayName("setSignupToken 테스트")
    class SetSignupTokenTests {

        @Test
        @DisplayName("회원가입 토큰 쿠키를 설정한다")
        void setSignupTokenSetsCookie() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-Proto", "https");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            authCookieService.setSignupToken(request, response, TOKEN, TOKEN_TTL);

            // Then
            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie).contains("signup_token=" + TOKEN);
            assertThat(setCookie).contains("Secure");
            assertThat(setCookie).contains("SameSite=None");
        }
    }

    @Nested
    @DisplayName("clearCookie 테스트")
    class ClearCookieTests {

        @Test
        @DisplayName("clearRefreshToken은 Max-Age=0 쿠키를 설정한다")
        void clearRefreshTokenSetsExpiredCookie() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            authCookieService.clearRefreshToken(request, response);

            // Then
            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie).contains("refresh_token=");
            assertThat(setCookie).contains("Max-Age=0");
        }

        @Test
        @DisplayName("clearSignupToken은 Max-Age=0 쿠키를 설정한다")
        void clearSignupTokenSetsExpiredCookie() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            authCookieService.clearSignupToken(request, response);

            // Then
            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie).contains("signup_token=");
            assertThat(setCookie).contains("Max-Age=0");
        }
    }

    @Nested
    @DisplayName("getCookieValue 테스트")
    class GetCookieValueTests {

        @Test
        @DisplayName("쿠키가 없으면 null을 반환한다")
        void getCookieValueWithoutCookiesReturnsNull() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();

            // When
            String value = authCookieService.getCookieValue(request, AuthCookieService.REFRESH_TOKEN_COOKIE);

            // Then
            assertThat(value).isNull();
        }

        @Test
        @DisplayName("동일한 이름의 쿠키가 있으면 값을 반환한다")
        void getCookieValueWithMatchedCookieReturnsValue() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(
                new Cookie("other", "x"),
                new Cookie(AuthCookieService.REFRESH_TOKEN_COOKIE, TOKEN)
            );

            // When
            String value = authCookieService.getCookieValue(request, AuthCookieService.REFRESH_TOKEN_COOKIE);

            // Then
            assertThat(value).isEqualTo(TOKEN);
        }

        @Test
        @DisplayName("요청한 이름의 쿠키가 없으면 null을 반환한다")
        void getCookieValueWithoutMatchedCookieReturnsNull() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("other", "x"));

            // When
            String value = authCookieService.getCookieValue(request, AuthCookieService.REFRESH_TOKEN_COOKIE);

            // Then
            assertThat(value).isNull();
        }
    }
}
