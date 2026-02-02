package gg.agit.konect.global.auth.oauth;

import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.service.RefreshTokenService;
import gg.agit.konect.domain.user.service.SignupTokenService;
import gg.agit.konect.global.auth.jwt.JwtProvider;
import gg.agit.konect.global.auth.web.AuthCookieService;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OAuthLoginOrchestrator {

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    private final ObjectProvider<NativeSessionBridgeService> nativeSessionBridgeService;
    private final OAuthLoginHelper oauthLoginHelper;

    private final JwtProvider jwtProvider;

    private final SignupTokenService signupTokenService;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieService authCookieService;

    public OAuthTokenLoginResponse loginOrSignup(
        HttpServletRequest request,
        HttpServletResponse response,
        Provider provider,
        String email,
        String providerId,
        String redirectUri
    ) {
        Optional<User> user = oauthLoginHelper.findUserByProvider(provider, email, providerId);

        if (user.isEmpty()) {
            if (provider == Provider.APPLE && !StringUtils.hasText(email)) {
                email = oauthLoginHelper.resolveAppleEmail(providerId);
                if (!StringUtils.hasText(email)) {
                    throw CustomException.of(ApiResponseCode.FAILED_EXTRACT_EMAIL);
                }
            }

            String token = signupTokenService.issue(email, provider, providerId);
            authCookieService.setSignupToken(request, response, token, signupTokenService.signupTtl());

            return OAuthTokenLoginResponse.signup(frontendBaseUrl + "/signup", token);
        }

        String safeRedirect = oauthLoginHelper.resolveSafeRedirect(redirectUri);

        String accessToken = jwtProvider.createToken(user.get().getId());

        if (oauthLoginHelper.isAppleOauthCallback(safeRedirect)) {
            NativeSessionBridgeService svc = nativeSessionBridgeService.getIfAvailable();
            if (svc != null) {
                String bridgeToken = svc.issue(user.get().getId());
                safeRedirect = oauthLoginHelper.appendBridgeToken(safeRedirect, bridgeToken);
            }

            authCookieService.clearRefreshToken(request, response);
            authCookieService.clearSignupToken(request, response);

            return OAuthTokenLoginResponse.login(safeRedirect, accessToken, null);
        }

        String refreshToken = refreshTokenService.issue(user.get().getId());
        authCookieService.setRefreshToken(request, response, refreshToken, refreshTokenService.refreshTtl());
        authCookieService.clearSignupToken(request, response);

        return OAuthTokenLoginResponse.login(safeRedirect, accessToken, refreshToken);
    }
}
