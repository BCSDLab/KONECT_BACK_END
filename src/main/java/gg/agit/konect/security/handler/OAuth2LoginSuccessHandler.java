package gg.agit.konect.security.handler;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import gg.agit.konect.security.enums.Provider;
import gg.agit.konect.user.model.User;
import gg.agit.konect.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    private final int TEMP_SESSION_EXPIRATION_SECONDS = 600;

    private final UserRepository userRepository;

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

    private void sendAdditionalInfoRequiredResponse(HttpServletRequest request, HttpServletResponse response,
        String email, Provider provider) throws IOException {
        HttpSession session = request.getSession(true);
        session.setAttribute("email", email);
        session.setAttribute("provider", provider);
        session.setMaxInactiveInterval(TEMP_SESSION_EXPIRATION_SECONDS);

        response.sendRedirect(frontendBaseUrl + "/signup");
    }

    private void sendLoginSuccessResponse(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getId());

        response.sendRedirect(frontendBaseUrl);
    }

    private String extractEmail(OAuth2User oauthUser, Provider provider) {
        Object current = oauthUser.getAttributes();

        for (String key : provider.getEmailPath().split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }

            current = map.get(key);
        }

        return (String)current;
    }
}
