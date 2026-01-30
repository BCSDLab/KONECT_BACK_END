package gg.agit.konect.global.auth.web;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuthCookieService {

    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    public static final String SIGNUP_TOKEN_COOKIE = "signup_token";

    private static final String COOKIE_PATH = "/";

    @Value("${app.cookie.domain:}")
    private String cookieDomain;

    public void setRefreshToken(HttpServletRequest request, HttpServletResponse response, String token, Duration ttl) {
        ResponseCookie cookie = baseCookie(request, REFRESH_TOKEN_COOKIE, token)
            .maxAge(ttl)
            .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void clearRefreshToken(HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie cookie = baseCookie(request, REFRESH_TOKEN_COOKIE, "")
            .maxAge(Duration.ZERO)
            .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void setSignupToken(HttpServletRequest request, HttpServletResponse response, String token, Duration ttl) {
        ResponseCookie cookie = baseCookie(request, SIGNUP_TOKEN_COOKIE, token)
            .maxAge(ttl)
            .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void clearSignupToken(HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie cookie = baseCookie(request, SIGNUP_TOKEN_COOKIE, "")
            .maxAge(Duration.ZERO)
            .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(HttpServletRequest request, String name, String value) {
        boolean secure = isSecureRequest(request);

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secure)
            .path(COOKIE_PATH);

        if (secure) {
            builder.sameSite("None");
        }

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        return builder;
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }

        String forwardedProto = request.getHeader("X-Forwarded-Proto");

        return "https".equalsIgnoreCase(forwardedProto);
    }
}
