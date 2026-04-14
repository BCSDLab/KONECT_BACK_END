package gg.agit.konect.global.ratelimit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rate Limit을 적용하기 위한 어노테이션.
 * 메서드 또는 클래스 레벨에 적용할 수 있습니다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 허용할 최대 요청 횟수.
     * 기본값: 50
     */
    int maxRequests() default 50;

    /**
     * 시간 윈도우 (초 단위).
     * 기본값: 600 (10분)
     */
    int timeWindowSeconds() default 600;

    /**
     * Rate Limit 키를 생성할 SpEL 표현식.
     * 메서드 파라미터를 참조할 수 있습니다. 예: #userId, #target
     * 기본값: 빈 문자열 (메서드 시그니처 기본 키 사용)
     */
    String keyExpression() default "";

}
