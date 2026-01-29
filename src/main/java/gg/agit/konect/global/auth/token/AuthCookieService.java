package gg.agit.konect.global.auth.token;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthCookieService {

    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    public static final String SIGNUP_TOKEN_COOKIE = "signup_token";

    private static final String COOKIE_PATH = "/";

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
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(isSecureRequest(request))
            .path(COOKIE_PATH);

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
