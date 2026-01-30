package gg.agit.konect.global.auth.web;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import gg.agit.konect.global.auth.jwt.JwtProvider;
import gg.agit.konect.global.auth.annotation.PublicApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LoginCheckInterceptor implements HandlerInterceptor {

    public static final String AUTHENTICATED_USER_ID_ATTRIBUTE = "authenticatedUserId";
    public static final String PUBLIC_ENDPOINT_ATTRIBUTE = "publicEndpoint";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (isPublicEndpoint(handlerMethod)) {
            request.setAttribute(PUBLIC_ENDPOINT_ATTRIBUTE, true);
            return true;
        }

        String accessToken = resolveBearerToken(request);
        Integer userId = jwtProvider.getUserId(accessToken);
        request.setAttribute(AUTHENTICATED_USER_ID_ATTRIBUTE, userId);

        return true;
    }

    private boolean isPublicEndpoint(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(PublicApi.class)
            || handlerMethod.getBeanType().isAnnotationPresent(PublicApi.class);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
    }
}
