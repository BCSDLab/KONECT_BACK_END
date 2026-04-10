package gg.agit.konect.unit.global.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.Cookie;

import gg.agit.konect.global.auth.web.AuthCookieService;

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
            .contains("Secure")
            .contains("SameSite=None");
        assertCommonCookieAttributes(setCookie);
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
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie)
            .contains("refresh_token=refresh-token")
            .contains("Max-Age=30")
            .contains("Secure")
            .contains("SameSite=None");
        assertCommonCookieAttributes(setCookie);
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
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie)
            .contains("signup_token=")
            .contains("Max-Age=0");
        assertCommonCookieAttributes(setCookie);
    }

    @Test
    @DisplayName("clearRefreshToken은 만료된 빈 쿠키를 내려준다")
    void clearRefreshTokenSetsExpiredEmptyCookie() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        authCookieService.clearRefreshToken(request, response);

        // then
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie)
            .contains("refresh_token=")
            .contains("Max-Age=0");
        assertCommonCookieAttributes(setCookie);
    }

    @Test
    @DisplayName("getCookieValue는 null 쿠키 배열을 처리한다")
    void getCookieValueHandlesNullCookieArray() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies((Cookie[])null); // 명시적으로 null 설정

        // when & then
        assertThat(authCookieService.getCookieValue(request, "any")).isNull();
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

    @Test
    @DisplayName("setRefreshToken은 null duration이면 예외를 던진다")
    void setRefreshTokenRejectsNullDuration() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> authCookieService.setRefreshToken(request, response, "refresh-token", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getCookieValue는 쿠키가 없으면 null을 반환한다")
    void getCookieValueReturnsNullWhenNoCookies() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 쿠키를 설정하지 않음

        // when & then
        assertThat(authCookieService.getCookieValue(request, "missing")).isNull();
    }

    @Test
    @DisplayName("isSecureRequest는 대소문자 혼합 HTTPS 헤더를 처리한다")
    void isSecureRequestHandlesMixedCaseHttpsHeader() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "HTTPS");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        authCookieService.setRefreshToken(request, response, "refresh-token", Duration.ofMinutes(5));

        // then
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).contains("Secure").contains("SameSite=None");
    }

    @Test
    @DisplayName("isSecureRequest는 X-Forwarded-Proto 헤더가 없고 request.isSecure()가 false이면 비보안으로 처리한다")
    void isSecureRequestTreatsMissingHeaderAndNonSecureRequestAsNonSecure() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        // X-Forwarded-Proto 헤더 없음
        // request.isSecure() 기본값은 false
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        authCookieService.setRefreshToken(request, response, "refresh-token", Duration.ofMinutes(5));

        // then
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).doesNotContain("Secure");
        assertThat(setCookie).doesNotContain("SameSite=None");
    }

    @Test
    @DisplayName("setSignupToken은 정상 동작한다")
    void setSignupTokenWorksNormally() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        authCookieService.setSignupToken(request, response, "signup-token", Duration.ofMinutes(5));

        // then
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie)
            .contains("signup_token=signup-token")
            .contains("Max-Age=300")
            .contains("Secure")
            .contains("SameSite=None");
        assertCommonCookieAttributes(setCookie);
    }

    @Test
    @DisplayName("setSignupToken과 clearSignupToken은 쿠키를 설정하고 제거한다")
    void setAndClearSignupTokenWorkTogether() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when - 설정
        authCookieService.setSignupToken(request, response, "signup-token", Duration.ofMinutes(5));
        String setCookie = response.getHeader("Set-Cookie");

        // then - 설정 확인
        assertThat(setCookie)
            .contains("signup_token=signup-token")
            .contains("Max-Age=300");

        // when - 제거
        MockHttpServletResponse clearResponse = new MockHttpServletResponse();
        authCookieService.clearSignupToken(request, clearResponse);
        String clearCookie = clearResponse.getHeader("Set-Cookie");

        // then - 제거 확인
        assertThat(clearCookie)
            .contains("signup_token=")
            .contains("Max-Age=0");
    }

    @Test
    @DisplayName("X-Forwarded-Proto 복수 값은 전체 문자열과 비교하므로 https가 아니면 비보안 처리한다")
    void xForwardedProtoMultipleValuesTreatsEntireString() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https,http");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        authCookieService.setRefreshToken(request, response, "refresh-token", Duration.ofMinutes(5));

        // then - 현재 구현에서는 전체 문자열과 비교하므로 "https,http"는 "https"와 다름
        String setCookie = response.getHeader("Set-Cookie");
        // equalsIgnoreCase("https")는 false이므로 비보안 처리
        assertThat(setCookie).doesNotContain("Secure");
        assertThat(setCookie).doesNotContain("SameSite=None");
    }

    @Test
    @DisplayName("X-Forwarded-Proto 복수 값은 첫 번째 값이 http이면 비보안 처리한다")
    void xForwardedProtoMultipleValuesTreatsHttpAsNonSecure() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "http,https");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        authCookieService.setRefreshToken(request, response, "refresh-token", Duration.ofMinutes(5));

        // then - 첫 번째 값이 http이므로 비보안 처리
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).doesNotContain("Secure");
        assertThat(setCookie).doesNotContain("SameSite=None");
    }

    private void assertCommonCookieAttributes(String setCookie) {
        assertThat(setCookie)
            .contains("Domain=konect.test")
            .contains("Path=/")
            .contains("HttpOnly");
    }
}
