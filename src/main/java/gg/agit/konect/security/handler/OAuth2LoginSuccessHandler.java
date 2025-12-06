package gg.agit.konect.security.handler;

import java.io.IOException;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.user.model.User;
import gg.agit.konect.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        OAuth2User oauthUser = (OAuth2User)authentication.getPrincipal();
        String email = (String)oauthUser.getAttributes().get("email");
        User user = userRepository.getByEmail(email);

        if (!user.getIsRegistered()) {
            sendAdditionalInfoRequiredResponse(response, email);
            return;
        }

        sendLoginSuccessResponse(request, response, user);
    }

    private void sendAdditionalInfoRequiredResponse(HttpServletResponse response, String email) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = Map.of(
            "status", "NEED_ADDITIONAL_INFO",
            "email", email
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private void sendLoginSuccessResponse(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getId());
        session.setAttribute("email", user.getEmail());

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = Map.of(
            "status", "SUCCESS",
            "userId", user.getId(),
            "email", user.getEmail()
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
