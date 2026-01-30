package gg.agit.konect.global.auth.oauth;

import java.io.IOException;
import java.net.URI;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class OAuth2RedirectUriSaveFilter extends OncePerRequestFilter {

    public static final String REDIRECT_URI_SESSION_KEY = "redirect_uri";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/oauth2/authorization/")) {
            String redirectUri = request.getParameter("redirect_uri");

            if (isValidRedirectUri(redirectUri)) {
                HttpSession session = request.getSession(true);
                session.setAttribute(REDIRECT_URI_SESSION_KEY, redirectUri);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidRedirectUri(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            return false;
        }

        try {
            URI uri = URI.create(redirectUri);

            if (uri.getScheme() == null || uri.getHost() == null) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
