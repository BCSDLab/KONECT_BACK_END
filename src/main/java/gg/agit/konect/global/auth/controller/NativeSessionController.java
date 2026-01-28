package gg.agit.konect.global.auth.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.global.auth.annotation.PublicApi;
import gg.agit.konect.global.auth.bridge.NativeSessionBridgeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class NativeSessionController {

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    private final NativeSessionBridgeService nativeSessionBridgeService;

    @PublicApi
    @GetMapping("/native/session/bridge")
    public void bridge(
        @RequestParam(name = "bridge_token", required = false) String bridgeToken,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        response.setHeader("Cache-Control", "no-store");

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

        HttpSession session = request.getSession(true);
        session.setAttribute("userId", userId);

        response.sendRedirect(frontendBaseUrl + "/home");
    }
}
