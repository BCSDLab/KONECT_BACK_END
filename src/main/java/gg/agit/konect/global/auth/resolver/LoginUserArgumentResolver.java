package gg.agit.konect.global.auth.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import gg.agit.konect.global.auth.annotation.UserId;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasAnnotation = parameter.hasParameterAnnotation(UserId.class);
        boolean isIntegerType = Integer.class.equals(parameter.getParameterType())
            || int.class.equals(parameter.getParameterType());

        return hasAnnotation && isIntegerType;
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = (HttpServletRequest)webRequest.getNativeRequest();
        HttpSession session = request.getSession(false);

        if (session == null) {
            throw CustomException.of(ApiResponseCode.INVALID_SESSION);
        }

        Object userId = session.getAttribute("userId");

        if (!(userId instanceof Integer id)) {
            throw CustomException.of(ApiResponseCode.INVALID_SESSION);
        }

        return id;
    }
}
