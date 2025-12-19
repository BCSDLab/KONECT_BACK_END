package gg.agit.konect.global.auth.interceptor;

import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import gg.agit.konect.global.auth.annotation.PublicApi;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    private static final List<String> PUBLIC_PATHS = List.of(
        "/oauth2/**",
        "/login/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**",
        "/swagger-resources/**",
        "/error"
    );

    private final PathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String requestUri = request.getRequestURI();
        boolean isAllowed = PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestUri));

        if (isAllowed) {
            return true;
        }

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (!isPublicEndpoint(handlerMethod)) {
            HttpSession session = request.getSession(false);
            Object userId = session == null ? null : session.getAttribute("userId");

            if (!(userId instanceof Integer)) {
                throw CustomException.of(ApiResponseCode.INVALID_SESSION);
            }
        }

        return true;
    }

    private boolean isPublicEndpoint(HandlerMethod handlerMethod) {
        boolean methodPublic = handlerMethod.hasMethodAnnotation(PublicApi.class);
        boolean controllerPublic = handlerMethod.getBeanType().isAnnotationPresent(PublicApi.class);

        return methodPublic || controllerPublic;
    }
}
