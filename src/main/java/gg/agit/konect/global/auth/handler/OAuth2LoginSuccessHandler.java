package gg.agit.konect.global.auth.handler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.config.SecurityProperties;
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

    private static final int TEMP_SESSION_EXPIRATION_SECONDS = 600;

    private final UserRepository userRepository;
    private final SecurityProperties securityProperties;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken)authentication;
        Provider provider = Provider.valueOf(oauthToken.getAuthorizedClientRegistrationId().toUpperCase());

        OAuth2User oauthUser = (OAuth2User)authentication.getPrincipal();
        String email = extractEmail(oauthUser, provider);

        Optional<User> user = userRepository.findByEmailAndProvider(email, provider);

        if (user.isEmpty()) {
            sendAdditionalInfoRequiredResponse(request, response, email, provider);
            return;
        }

        sendLoginSuccessResponse(request, response, user.get());
    }

    private void sendAdditionalInfoRequiredResponse(
        HttpServletRequest request,
        HttpServletResponse response,
        String email,
        Provider provider
    ) throws IOException {
        HttpSession session = request.getSession(true);
        session.setAttribute("email", email);
        session.setAttribute("provider", provider);
        session.setMaxInactiveInterval(TEMP_SESSION_EXPIRATION_SECONDS);

        response.sendRedirect(frontendBaseUrl + "/signup");
    }

    private void sendLoginSuccessResponse(
        HttpServletRequest request,
        HttpServletResponse response,
        User user
    ) throws IOException {
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getId());

        String redirectUri = (String)session.getAttribute("redirect_uri");
        session.removeAttribute("redirect_uri");

        response.sendRedirect(resolveSafeRedirect(redirectUri));
    }

    private String extractEmail(OAuth2User oauthUser, Provider provider) {
        Object current = oauthUser.getAttributes();

        for (String key : provider.getEmailPath().split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                throw CustomException.of(ApiResponseCode.FAILED_EXTRACT_EMAIL);
            }

            current = map.get(key);
        }

        return (String)current;
    }

    private String resolveSafeRedirect(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            return frontendBaseUrl + "/home";
        }

        Set<String> allowedOrigins = securityProperties.getAllowedRedirectOrigins();

        try {
            URI uri = URI.create(redirectUri);

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
