package gg.agit.konect.global.auth.web;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;

import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.auth.annotation.Auth;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 권한 체크 인터셉터.
 * @Auth 어노테이션이 있는 경우 사용자의 역할(Role)을 검증합니다.
 * 예외 발생 시 HandlerExceptionResolver를 통해 GlobalExceptionHandler로 위임합니다.
 */
@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public AuthorizationInterceptor(
            UserRepository userRepository,
            HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.userRepository = userRepository;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (Boolean.TRUE.equals(request.getAttribute(LoginCheckInterceptor.PUBLIC_ENDPOINT_ATTRIBUTE))) {
            return true;
        }

        Auth auth = findAuthAnnotation(handlerMethod);

        if (auth == null) {
            return true;
        }

        try {
            Object userId = request.getAttribute(LoginCheckInterceptor.AUTHENTICATED_USER_ID_ATTRIBUTE);

            if (!(userId instanceof Integer id)) {
                throw CustomException.of(ApiResponseCode.MISSING_ACCESS_TOKEN);
            }

            validateRole(id, auth);
            return true;
        } catch (CustomException e) {
            handlerExceptionResolver.resolveException(request, response, handler, e);
            return false;
        }
    }

    private Auth findAuthAnnotation(HandlerMethod handlerMethod) {
        Auth methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(
            handlerMethod.getMethod(), Auth.class);

        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        return AnnotatedElementUtils.findMergedAnnotation(
            handlerMethod.getBeanType(), Auth.class);
    }

    // 요청자의 권한과 @Auth에서 지정된 권한을 비교하여 검증
    private void validateRole(Integer userId, Auth auth) {
        User user = userRepository.getById(userId);

        for (var allowedRole : auth.roles()) {
            if (user.getRole() == allowedRole) {
                return;
            }
        }

        throw CustomException.of(ApiResponseCode.FORBIDDEN_ROLE_ACCESS);
    }
}
