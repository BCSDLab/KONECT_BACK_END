package gg.agit.konect.global.auth.bridge;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.global.auth.annotation.PublicApi;
import gg.agit.konect.domain.user.service.RefreshTokenService;
import gg.agit.konect.global.auth.token.AuthCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Profile("!local")
@RestController
@RequiredArgsConstructor
public class NativeSessionController {

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    private final NativeSessionBridgeService nativeSessionBridgeService;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieService authCookieService;

    @PublicApi
    @GetMapping("/native/session/bridge")
    public void bridge(
        @RequestParam(name = "bridge_token", required = false) String bridgeToken,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

        if (!StringUtils.hasText(bridgeToken)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        Integer userId = nativeSessionBridgeService.consume(bridgeToken).orElse(null);

        if (userId == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }

        authCookieService.clearSignupToken(request, response);

        String refreshToken = refreshTokenService.issue(userId);
        authCookieService.setRefreshToken(request, response, refreshToken, refreshTokenService.refreshTtl());

        response.sendRedirect(frontendBaseUrl + "/home");
    }
}
