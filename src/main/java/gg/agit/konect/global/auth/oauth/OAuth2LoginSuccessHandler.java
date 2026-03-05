package gg.agit.konect.global.auth.oauth;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.UnRegisteredUser;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UnRegisteredUserRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.domain.user.service.RefreshTokenService;
import gg.agit.konect.domain.user.service.SignupTokenService;
import gg.agit.konect.domain.user.service.UserOAuthAccountService;
import gg.agit.konect.global.auth.web.AuthCookieService;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    private final ObjectProvider<NativeSessionBridgeService> nativeSessionBridgeService;
    private final OAuthLoginHelper oauthLoginHelper;
    private final AppleOAuthNameResolver appleOAuthNameResolver;
    private final ObjectMapper objectMapper;
    private final SignupTokenService signupTokenService;
    private final RefreshTokenService refreshTokenService;
    private final UserOAuthAccountService userOAuthAccountService;
    private final AuthCookieService authCookieService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UserRepository userRepository;
    private final UnRegisteredUserRepository unRegisteredUserRepository;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken)authentication;
        Provider provider = Provider.valueOf(oauthToken.getAuthorizedClientRegistrationId().toUpperCase());
        OAuth2User oauthUser = (OAuth2User)authentication.getPrincipal();

        String providerId = extractProviderId(oauthUser, provider);
        String email = extractEmail(oauthUser, provider);
        String name = resolveAppleName(request, provider, oauthUser);
        OAuthContext oauthContext = consumeOAuthContext(request);

        if (!StringUtils.hasText(providerId)) {
            throw CustomException.of(ApiResponseCode.FAILED_EXTRACT_PROVIDER_ID);
        }

        String appleRefreshTokenValue = extractAppleRefreshToken(oauthToken);

        if (oauthContext.linkMode()) {
            handleOAuthLinkMode(
                request,
                response,
                oauthContext.safeRedirect(),
                provider,
                providerId,
                email,
                appleRefreshTokenValue
            );
            return;
        }

        Optional<User> user;

        user = oauthLoginHelper.findUserByProvider(provider, email, providerId);

        if (user.isEmpty()) {
            if (provider == Provider.APPLE && !StringUtils.hasText(email)) {
                email = oauthLoginHelper.resolveAppleEmail(providerId);

                if (!StringUtils.hasText(email)) {
                    throw CustomException.of(ApiResponseCode.FAILED_EXTRACT_EMAIL);
                }
            }

            saveAppleRefreshTokenForUnRegisteredUser(provider, providerId, email, name, appleRefreshTokenValue);
            sendAdditionalInfoRequiredResponse(request, response, email, provider, providerId, name);
            return;
        }

        saveAppleRefreshTokenForUser(provider, user.get(), appleRefreshTokenValue);
        sendLoginSuccessResponse(request, response, user.get(), oauthContext.safeRedirect());
    }

    private void sendAdditionalInfoRequiredResponse(
        HttpServletRequest request,
        HttpServletResponse response,
        String email,
        Provider provider,
        String providerId,
        String name
    ) throws IOException {
        String token = signupTokenService.issue(email, provider, providerId, name);
        authCookieService.setSignupToken(request, response, token, signupTokenService.signupTtl());
        response.sendRedirect(frontendBaseUrl + "/signup");
    }

    private void sendLoginSuccessResponse(
        HttpServletRequest request,
        HttpServletResponse response,
        User user,
        String safeRedirect
    ) throws IOException {
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

    private void handleOAuthLinkMode(
        HttpServletRequest request,
        HttpServletResponse response,
        String safeRedirect,
        Provider provider,
        String providerId,
        String email,
        String appleRefreshToken
    ) throws IOException {
        Integer currentUserId = resolveCurrentUserId(request);

        if (currentUserId == null) {
            String failedRedirect = oauthLoginHelper.appendQueryParameter(safeRedirect, "oauth_link", "failed");
            response.sendRedirect(
                oauthLoginHelper.appendQueryParameter(
                    failedRedirect,
                    "oauth_link_code",
                    ApiResponseCode.INVALID_SESSION.getCode()
                )
            );
            return;
        }

        try {
            userOAuthAccountService.linkOAuthAccount(currentUserId, provider, providerId, email);

            if (provider == Provider.APPLE && StringUtils.hasText(appleRefreshToken)) {
                try {
                    User user = userRepository.getById(currentUserId);
                    saveAppleRefreshTokenForUser(provider, user, appleRefreshToken);
                } catch (Exception exception) {
                    log.warn(
                        "OAuth link succeeded but failed to save apple refresh token. userId={}",
                        currentUserId,
                        exception
                    );
                }
            }

            response.sendRedirect(oauthLoginHelper.appendQueryParameter(safeRedirect, "oauth_link", "success"));
        } catch (CustomException exception) {
            String failedRedirect = oauthLoginHelper.appendQueryParameter(safeRedirect, "oauth_link", "failed");
            response.sendRedirect(
                oauthLoginHelper.appendQueryParameter(
                    failedRedirect,
                    "oauth_link_code",
                    exception.getErrorCode().getCode()
                )
            );
        } catch (Exception exception) {
            log.error("Unexpected error during oauth link callback", exception);
            String failedRedirect = oauthLoginHelper.appendQueryParameter(safeRedirect, "oauth_link", "failed");
            response.sendRedirect(
                oauthLoginHelper.appendQueryParameter(
                    failedRedirect,
                    "oauth_link_code",
                    ApiResponseCode.UNEXPECTED_SERVER_ERROR.getCode()
                )
            );
        }
    }

    private Integer resolveCurrentUserId(HttpServletRequest request) {
        String refreshToken = authCookieService.getCookieValue(request, AuthCookieService.REFRESH_TOKEN_COOKIE);

        if (!StringUtils.hasText(refreshToken)) {
            return null;
        }

        try {
            return refreshTokenService.extractUserId(refreshToken);
        } catch (CustomException exception) {
            return null;
        }
    }

    private OAuthContext consumeOAuthContext(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String redirectUri = null;
        String oauthMode = null;

        if (session != null) {
            redirectUri = (String)session.getAttribute(OAuth2RedirectUriSaveFilter.REDIRECT_URI_SESSION_KEY);
            oauthMode = (String)session.getAttribute(OAuth2RedirectUriSaveFilter.OAUTH_MODE_SESSION_KEY);
            session.removeAttribute(OAuth2RedirectUriSaveFilter.REDIRECT_URI_SESSION_KEY);
            session.removeAttribute(OAuth2RedirectUriSaveFilter.OAUTH_MODE_SESSION_KEY);
        }

        return new OAuthContext(
            oauthLoginHelper.resolveSafeRedirect(redirectUri),
            OAuth2RedirectUriSaveFilter.OAUTH_MODE_LINK.equalsIgnoreCase(oauthMode)
        );
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

    private String extractProviderId(OAuth2User oauthUser, Provider provider) {
        if (provider == Provider.APPLE) {
            String providerId = oauthUser.getAttribute("sub");

            if (!StringUtils.hasText(providerId)) {
                providerId = oauthUser.getName();
            }

            return providerId;
        }

        return oauthUser.getName();
    }

    private String resolveAppleName(HttpServletRequest request, Provider provider, OAuth2User oauthUser) {
        if (provider != Provider.APPLE) {
            return null;
        }

        String extracted = appleOAuthNameResolver.resolve(oauthUser.getAttributes());

        if (StringUtils.hasText(extracted)) {
            return extracted;
        }

        return extractAppleNameFromUserPayload(request);
    }

    private String extractAppleNameFromUserPayload(HttpServletRequest request) {
        String userPayload = request.getParameter("user");

        if (!StringUtils.hasText(userPayload)) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(userPayload);
            JsonNode name = root.path("name");
            String firstName = asText(name.path("firstName"));
            String lastName = asText(name.path("lastName"));

            if (StringUtils.hasText(firstName) && StringUtils.hasText(lastName)) {
                return lastName + firstName;
            }

            if (StringUtils.hasText(firstName)) {
                return firstName;
            }

            if (StringUtils.hasText(lastName)) {
                return lastName;
            }
        } catch (Exception e) {
            log.debug("Failed to parse Apple user payload", e);
        }

        return null;
    }

    private String asText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        String value = node.asText();
        return StringUtils.hasText(value) ? value : null;
    }

    private String extractAppleRefreshToken(OAuth2AuthenticationToken oauthToken) {
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        if (!Provider.APPLE.name().equalsIgnoreCase(registrationId)) {
            return null;
        }

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            registrationId, oauthToken.getName()
        );

        if (client == null) {
            return null;
        }

        OAuth2RefreshToken refreshToken = client.getRefreshToken();
        return refreshToken != null ? refreshToken.getTokenValue() : null;
    }

    private void saveAppleRefreshTokenForUser(Provider provider, User user, String appleRefreshToken) {
        if (provider != Provider.APPLE || !StringUtils.hasText(appleRefreshToken)) {
            return;
        }

        user.updateAppleRefreshToken(appleRefreshToken);
        userRepository.save(user);
    }

    private void saveAppleRefreshTokenForUnRegisteredUser(
        Provider provider, String providerId, String email, String name, String appleRefreshToken
    ) {
        if (provider != Provider.APPLE) {
            return;
        }

        Optional<UnRegisteredUser> unRegisteredUser;

        if (StringUtils.hasText(providerId)) {
            unRegisteredUser = unRegisteredUserRepository.findByProviderIdAndProvider(providerId, Provider.APPLE);
        } else {
            unRegisteredUser = unRegisteredUserRepository.findByEmailAndProvider(email, Provider.APPLE);
        }

        unRegisteredUser.ifPresent(u -> {
            if (StringUtils.hasText(appleRefreshToken)) {
                u.updateAppleRefreshToken(appleRefreshToken);
            }
            if (StringUtils.hasText(name)) {
                u.updateName(name);
            }
            unRegisteredUserRepository.save(u);
        });
    }

    private record OAuthContext(String safeRedirect, boolean linkMode) {
    }
}
