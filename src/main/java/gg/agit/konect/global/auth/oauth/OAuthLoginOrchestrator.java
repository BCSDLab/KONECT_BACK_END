package gg.agit.konect.global.auth.oauth;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.UnRegisteredUser;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UnRegisteredUserRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.domain.user.service.RefreshTokenService;
import gg.agit.konect.domain.user.service.SignupTokenService;
import gg.agit.konect.global.auth.jwt.JwtProvider;
import gg.agit.konect.global.auth.web.AuthCookieService;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.config.SecurityProperties;
import gg.agit.konect.global.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OAuthLoginOrchestrator {

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    private final UserRepository userRepository;
    private final UnRegisteredUserRepository unRegisteredUserRepository;
    private final SecurityProperties securityProperties;
    private final ObjectProvider<NativeSessionBridgeService> nativeSessionBridgeService;

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
        Optional<User> user = findUserByProvider(provider, email, providerId);

        if (user.isEmpty()) {
            if (provider == Provider.APPLE && !StringUtils.hasText(email)) {
                email = resolveAppleEmail(providerId);
                if (!StringUtils.hasText(email)) {
                    throw CustomException.of(ApiResponseCode.FAILED_EXTRACT_EMAIL);
                }
            }

            String token = signupTokenService.issue(email, provider, providerId);
            authCookieService.setSignupToken(request, response, token, signupTokenService.signupTtl());

            return OAuthTokenLoginResponse.signup(frontendBaseUrl + "/signup", token);
        }

        String safeRedirect = resolveSafeRedirect(redirectUri);

        String accessToken = jwtProvider.createToken(user.get().getId());

        if (isAppleOauthCallback(safeRedirect)) {
            NativeSessionBridgeService svc = nativeSessionBridgeService.getIfAvailable();
            if (svc != null) {
                String bridgeToken = svc.issue(user.get().getId());
                safeRedirect = appendBridgeToken(safeRedirect, bridgeToken);
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

    private Optional<User> findUserByProvider(Provider provider, String email, String providerId) {
        if (provider == Provider.APPLE) {
            return userRepository.findByProviderIdAndProvider(providerId, provider);
        }
        return userRepository.findByEmailAndProvider(email, provider);
    }

    private String resolveAppleEmail(String providerId) {
        if (!StringUtils.hasText(providerId)) {
            return null;
        }

        return unRegisteredUserRepository.findByProviderIdAndProvider(providerId, Provider.APPLE)
            .map(UnRegisteredUser::getEmail)
            .orElse(null);
    }

    private boolean isAppleOauthCallback(String redirectUri) {
        return redirectUri != null && redirectUri.startsWith("konect://oauth/callback");
    }

    private String appendBridgeToken(String redirectUri, String bridgeToken) {
        if (redirectUri.contains("bridge_token=")) {
            return redirectUri;
        }
        char joiner = redirectUri.contains("?") ? '&' : '?';
        return redirectUri + joiner + "bridge_token=" + bridgeToken;
    }

    private String resolveSafeRedirect(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            return frontendBaseUrl + "/home";
        }

        Set<String> allowedOrigins = securityProperties.getAllowedRedirectOrigins();

        try {
            URI uri = URI.create(redirectUri);

            if ("konect".equalsIgnoreCase(uri.getScheme()) && "oauth".equalsIgnoreCase(uri.getHost())) {
                return redirectUri;
            }

            if (uri.getScheme() == null || uri.getHost() == null) {
                return frontendBaseUrl + "/home";
            }

            String origin = uri.getScheme() + "://" + uri.getHost() + portPart(uri);

            if (allowedOrigins.contains(origin)) {
                return redirectUri;
            }
        } catch (Exception ignored) {
        }

        return frontendBaseUrl + "/home";
    }

    private String portPart(URI uri) {
        return (uri.getPort() == -1) ? "" : ":" + uri.getPort();
    }
}
