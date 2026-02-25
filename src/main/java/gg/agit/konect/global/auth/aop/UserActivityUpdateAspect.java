package gg.agit.konect.global.auth.aop;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import gg.agit.konect.domain.user.service.UserActivityService;
import gg.agit.konect.global.auth.web.LoginCheckInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class UserActivityUpdateAspect {

    private final UserActivityService userActivityService;

    @After("execution(* gg.agit.konect..controller..*(..))")
    public void updateLastActivity() {
        ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        Object userId = request.getAttribute(LoginCheckInterceptor.AUTHENTICATED_USER_ID_ATTRIBUTE);
        if (userId instanceof Integer authenticatedUserId) {
            userActivityService.updateLastActivityAt(authenticatedUserId);
        }
    }
}
