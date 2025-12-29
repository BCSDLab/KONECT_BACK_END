package gg.agit.konect.global.auth.interceptor;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (isPublicEndpoint(handlerMethod)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute("userId");

        if (!(userId instanceof Integer)) {
            throw CustomException.of(ApiResponseCode.INVALID_SESSION);
        }

        return true;
    }

    private boolean isPublicEndpoint(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(PublicApi.class)
            || handlerMethod.getBeanType().isAnnotationPresent(PublicApi.class);
    }
}
