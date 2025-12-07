package gg.agit.konect.security.handler;

import java.io.IOException;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.security.dto.AdditionalInfoRequiredResponse;
import gg.agit.konect.security.dto.LoginSuccessResponse;
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

    private final int TEMP_SESSION_EXPIRATION_SECONDS = 600;

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        Provider provider = Provider.valueOf(oauthToken.getAuthorizedClientRegistrationId().toUpperCase());

        OAuth2User oauthUser = (OAuth2User)authentication.getPrincipal();
        String email = (String)oauthUser.getAttributes().get("email");

        Optional<User> user = userRepository.findByEmailAndProvider(email, provider);

        if (user.isEmpty()) {
            sendAdditionalInfoRequiredResponse(request, response, email, provider);
            return;
        }

        sendLoginSuccessResponse(request, response, user.get(), provider);
    }

    private void sendAdditionalInfoRequiredResponse(HttpServletRequest request, HttpServletResponse response, String email, Provider provider) throws IOException {
        HttpSession session = request.getSession(true);
        session.setAttribute("email", email);
        session.setAttribute("provider", provider);
        session.setAttribute("isRegistered", false);
        session.setMaxInactiveInterval(TEMP_SESSION_EXPIRATION_SECONDS);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");

        AdditionalInfoRequiredResponse body = AdditionalInfoRequiredResponse.of(email, provider);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private void sendLoginSuccessResponse(HttpServletRequest request, HttpServletResponse response, User user, Provider provider) throws IOException {
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getId());
        session.setAttribute("email", user.getEmail());
        session.setAttribute("provider", provider);
        session.setAttribute("isRegistered", true);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");

        LoginSuccessResponse body = LoginSuccessResponse.of(user.getId(), user.getEmail(), provider);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
