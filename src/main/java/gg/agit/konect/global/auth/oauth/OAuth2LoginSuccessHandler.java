package gg.agit.konect.global.auth.oauth;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.service.RefreshTokenService;
import gg.agit.konect.domain.user.service.SignupTokenService;
import gg.agit.konect.global.auth.web.AuthCookieService;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    private final ObjectProvider<NativeSessionBridgeService> nativeSessionBridgeService;
    private final OAuthLoginHelper oauthLoginHelper;
    private final SignupTokenService signupTokenService;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieService authCookieService;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken)authentication;
        Provider provider = Provider.valueOf(oauthToken.getAuthorizedClientRegistrationId().toUpperCase());
        OAuth2User oauthUser = (OAuth2User)authentication.getPrincipal();

        String providerId = null;
        String email = extractEmail(oauthUser, provider);
        Optional<User> user;

        if (provider == Provider.APPLE) {
            providerId = extractProviderId(oauthUser);

            if (!StringUtils.hasText(providerId)) {
                throw CustomException.of(ApiResponseCode.FAILED_EXTRACT_PROVIDER_ID);
            }
        }

        user = oauthLoginHelper.findUserByProvider(provider, email, providerId);

        if (user.isEmpty()) {
            if (provider == Provider.APPLE && !StringUtils.hasText(email)) {
                email = oauthLoginHelper.resolveAppleEmail(providerId);

                if (!StringUtils.hasText(email)) {
                    throw CustomException.of(ApiResponseCode.FAILED_EXTRACT_EMAIL);
                }
            }

            sendAdditionalInfoRequiredResponse(request, response, email, provider, providerId);
            return;
        }

        sendLoginSuccessResponse(request, response, user.get());
    }

    private void sendAdditionalInfoRequiredResponse(
        HttpServletRequest request,
        HttpServletResponse response,
        String email,
        Provider provider,
        String providerId
    ) throws IOException {
        String token = signupTokenService.issue(email, provider, providerId);
        authCookieService.setSignupToken(request, response, token, signupTokenService.signupTtl());
        response.sendRedirect(frontendBaseUrl + "/signup");
    }

    private void sendLoginSuccessResponse(
        HttpServletRequest request,
        HttpServletResponse response,
        User user
    ) throws IOException {
        HttpSession session = request.getSession(false);
        String redirectUri = session == null ? null : (String)session.getAttribute("redirect_uri");
        if (session != null) {
            session.removeAttribute("redirect_uri");
        }

        String safeRedirect = oauthLoginHelper.resolveSafeRedirect(redirectUri);

        if (oauthLoginHelper.isAppleOauthCallback(safeRedirect)) {
            NativeSessionBridgeService svc = nativeSessionBridgeService.getIfAvailable();

            if (svc != null) {
                String bridgeToken = svc.issue(user.getId());
                safeRedirect = oauthLoginHelper.appendBridgeToken(safeRedirect, bridgeToken);
            }

            authCookieService.clearRefreshToken(request, response);
            authCookieService.clearSignupToken(request, response);

            response.sendRedirect(safeRedirect);
            return;
        }

        String refreshToken = refreshTokenService.issue(user.getId());
        authCookieService.setRefreshToken(request, response, refreshToken, refreshTokenService.refreshTtl());

        authCookieService.clearSignupToken(request, response);

        response.sendRedirect(safeRedirect);
    }

    private String extractEmail(OAuth2User oauthUser, Provider provider) {
        Object current = oauthUser.getAttributes();
        boolean allowMissing = provider == Provider.APPLE;

        for (String key : provider.getEmailPath().split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                if (allowMissing) {
                    return null;
                }

                throw CustomException.of(ApiResponseCode.FAILED_EXTRACT_EMAIL);
            }

            current = map.get(key);
        }

        if (current == null && allowMissing) {
            return null;
        }

        return (String)current;
    }

    private String extractProviderId(OAuth2User oauthUser) {
        String providerId = oauthUser.getAttribute("sub");

        if (!StringUtils.hasText(providerId)) {
            providerId = oauthUser.getName();
        }

        return providerId;
    }
}
