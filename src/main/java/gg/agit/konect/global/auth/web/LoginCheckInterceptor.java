package gg.agit.konect.global.auth.web;

import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;

import gg.agit.konect.global.auth.jwt.JwtProvider;
import gg.agit.konect.global.auth.annotation.PublicApi;
import gg.agit.konect.global.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그인 체크 인터셉터.
 * JWT 액세스 토큰을 검증하고 인증된 사용자 ID를 request attribute에 설정합니다.
 * @PublicApi 어노테이션이 있는 경우 인증을 건너뜁니다.
 *
 * 예외 발생 시 HandlerExceptionResolver를 통해 GlobalExceptionHandler로 위임합니다.
 */
@Slf4j
@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    public static final String AUTHENTICATED_USER_ID_ATTRIBUTE = "authenticatedUserId";
    public static final String PUBLIC_ENDPOINT_ATTRIBUTE = "publicEndpoint";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public LoginCheckInterceptor(
        JwtProvider jwtProvider,
        @Lazy HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.jwtProvider = jwtProvider;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws
        Exception {
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

        try {
            String accessToken = resolveBearerToken(request);
            Integer userId = jwtProvider.getUserId(accessToken);
            request.setAttribute(AUTHENTICATED_USER_ID_ATTRIBUTE, userId);
            return true;
        } catch (CustomException e) {
            if (isSseRequest(request, handlerMethod)) {
                log.warn(
                    "SSE authentication failed: method={} uri={} status={} code={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    e.getErrorCode().getHttpStatus().value(),
                    e.getErrorCode().getCode()
                );
                response.setStatus(e.getErrorCode().getHttpStatus().value());
                return false;
            }

            // GlobalExceptionHandler가 처리하도록 위임
            handlerExceptionResolver.resolveException(request, response, handler, e);
            return false;
        }
    }

    private boolean isSseRequest(HttpServletRequest request, HandlerMethod handlerMethod) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE)) {
            return true;
        }

        GetMapping getMapping = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), GetMapping.class);
        if (getMapping != null) {
            for (String producedType : getMapping.produces()) {
                if (MediaType.TEXT_EVENT_STREAM_VALUE.equals(producedType)) {
                    return true;
                }
            }
        }

        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(
            handlerMethod.getMethod(), RequestMapping.class);
        if (requestMapping == null) {
            return false;
        }

        for (String producedType : requestMapping.produces()) {
            if (MediaType.TEXT_EVENT_STREAM_VALUE.equals(producedType)) {
                return true;
            }
        }

        return false;
    }

    private boolean isPublicEndpoint(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.findMergedAnnotation(
            handlerMethod.getMethod(), PublicApi.class) != null
            || AnnotatedElementUtils.findMergedAnnotation(
            handlerMethod.getBeanType(), PublicApi.class) != null;
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
    }
}
