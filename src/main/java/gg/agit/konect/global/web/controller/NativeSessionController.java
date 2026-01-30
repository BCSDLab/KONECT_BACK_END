package gg.agit.konect.global.web.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.user.service.RefreshTokenService;
import gg.agit.konect.global.auth.annotation.PublicApi;
import gg.agit.konect.global.auth.bridge.NativeSessionBridgeService;
import gg.agit.konect.global.auth.token.AuthCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class NativeSessionController {

    private final NativeSessionBridgeService nativeSessionBridgeService;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieService authCookieService;

    @PublicApi
    @GetMapping("/native/session/bridge")
    public ResponseEntity<Map<String, Boolean>> bridge(
        @RequestParam(name = "bridge_token", required = false) String bridgeToken,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

        if (!StringUtils.hasText(bridgeToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Integer userId = nativeSessionBridgeService.consume(bridgeToken).orElse(null);

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }

        authCookieService.clearSignupToken(request, response);

        String refreshToken = refreshTokenService.issue(userId);
        authCookieService.setRefreshToken(request, response, refreshToken, refreshTokenService.refreshTtl());

        return ResponseEntity.ok(Map.of("success", true));
    }
}
