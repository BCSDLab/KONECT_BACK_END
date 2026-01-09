package gg.agit.konect.global.auth.interceptor;

import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CLUB_MANAGER_ACCESS;
import static gg.agit.konect.global.code.ApiResponseCode.INVALID_SESSION;
import static gg.agit.konect.global.code.ApiResponseCode.INVALID_TYPE_VALUE;
import static gg.agit.konect.global.code.ApiResponseCode.MISSING_REQUIRED_PARAMETER;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import gg.agit.konect.domain.club.enums.ClubPositionGroup;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.global.auth.annotation.ClubManagerOnly;
import gg.agit.konect.global.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClubManagerCheckInterceptor implements HandlerInterceptor {

    private static final Set<ClubPositionGroup> MANAGER_ALLOWED_GROUPS =
        EnumSet.of(ClubPositionGroup.PRESIDENT, ClubPositionGroup.MANAGER);

    private final ClubMemberRepository clubMemberRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (!requiresClubManager(handlerMethod)) {
            return true;
        }

        Integer userId = getUserId(request);
        Integer clubId = getClubId(request);

        boolean isManager = clubMemberRepository.existsByClubIdAndUserIdAndPositionGroupIn(
            clubId,
            userId,
            MANAGER_ALLOWED_GROUPS
        );

        if (!isManager) {
            throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        }

        return true;
    }

    private boolean requiresClubManager(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(ClubManagerOnly.class)
            || handlerMethod.getBeanType().isAnnotationPresent(ClubManagerOnly.class);
    }

    private Integer getUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute("userId");

        if (!(userId instanceof Integer)) {
            throw CustomException.of(INVALID_SESSION);
        }

        return (Integer)userId;
    }

    private Integer getClubId(HttpServletRequest request) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        if (!(attribute instanceof Map<?, ?> pathVariables)) {
            throw CustomException.of(MISSING_REQUIRED_PARAMETER);
        }

        Object clubIdValue = pathVariables.get("clubId");

        if (!(clubIdValue instanceof String clubIdText) || !StringUtils.hasText(clubIdText)) {
            throw CustomException.of(MISSING_REQUIRED_PARAMETER);
        }

        try {
            return Integer.valueOf(clubIdText);
        } catch (NumberFormatException ex) {
            throw CustomException.of(INVALID_TYPE_VALUE);
        }
    }
}
