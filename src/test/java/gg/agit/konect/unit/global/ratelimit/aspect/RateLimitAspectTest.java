package gg.agit.konect.unit.global.ratelimit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.global.ratelimit.annotation.RateLimit;
import gg.agit.konect.global.ratelimit.aspect.RateLimitAspect;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private RateLimit rateLimit;

    @InjectMocks
    private RateLimitAspect rateLimitAspect;

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("제한 내 요청 시 정상 통과")
    void allowsRequestWithinLimit() throws Throwable {
        // given
        given(rateLimit.maxRequests()).willReturn(10);
        given(rateLimit.timeWindowSeconds()).willReturn(60);
        given(rateLimit.keyExpression()).willReturn("#userId");
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getDeclaringTypeName()).willReturn("test");
        given(methodSignature.getName()).willReturn("method");
        given(methodSignature.getParameterNames()).willReturn(new String[]{"userId"});
        given(joinPoint.getArgs()).willReturn(new Object[]{"user123"});

        // Lua 스크립트 실행 결과 모킹
        when(redisTemplate.execute(any(DefaultRedisScript.class), any(List.class), any(String.class)))
            .thenReturn(5L);

        Object expectedResult = new Object();
        given(joinPoint.proceed()).willReturn(expectedResult);

        // when
        Object result = rateLimitAspect.around(joinPoint, rateLimit);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Lua 스크립트로 원자적 INCR + TTL 설정")
    void usesLuaScriptForAtomicIncrAndTtl() throws Throwable {
        // given
        given(rateLimit.maxRequests()).willReturn(10);
        given(rateLimit.timeWindowSeconds()).willReturn(60);
        given(rateLimit.keyExpression()).willReturn("#userId");
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getDeclaringTypeName()).willReturn("test");
        given(methodSignature.getName()).willReturn("method");
        given(methodSignature.getParameterNames()).willReturn(new String[]{"userId"});
        given(joinPoint.getArgs()).willReturn(new Object[]{"user123"});
        given(joinPoint.proceed()).willReturn(null);

        when(redisTemplate.execute(any(DefaultRedisScript.class), any(List.class), any(String.class)))
            .thenReturn(1L);

        // when
        rateLimitAspect.around(joinPoint, rateLimit);

        // then - Lua 스크립트가 실행되었는지 검증
        verify(redisTemplate).execute(any(DefaultRedisScript.class), any(List.class), eq("60"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("제한 초과 시 RateLimitExceededException 발생")
    void throwsExceptionWhenLimitExceeded() {
        // given
        given(rateLimit.maxRequests()).willReturn(10);
        given(rateLimit.timeWindowSeconds()).willReturn(60);
        given(rateLimit.keyExpression()).willReturn("#userId");
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getDeclaringTypeName()).willReturn("test");
        given(methodSignature.getName()).willReturn("method");
        given(methodSignature.getParameterNames()).willReturn(new String[]{"userId"});
        given(joinPoint.getArgs()).willReturn(new Object[]{"user123"});

        // Lua 스크립트가 카운터를 11로 반환 (제한 초과)
        when(redisTemplate.execute(any(DefaultRedisScript.class), any(List.class), any(String.class)))
            .thenReturn(11L);
        given(redisTemplate.getExpire("ratelimit:test.method:user123")).willReturn(30L);

        // when & then
        assertThatThrownBy(() -> rateLimitAspect.around(joinPoint, rateLimit))
            .isInstanceOf(CustomException.class)
            .satisfies(ex -> {
                CustomException customEx = (CustomException)ex;
                assertThat(customEx.getErrorCode()).isEqualTo(ApiResponseCode.TOO_MANY_REQUESTS);
            });
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("빈 keyExpression 시 메서드 시그니처 기본 키 사용")
    void usesDefaultKeyWhenExpressionIsEmpty() throws Throwable {
        // given
        given(rateLimit.maxRequests()).willReturn(10);
        given(rateLimit.timeWindowSeconds()).willReturn(60);
        given(rateLimit.keyExpression()).willReturn("");
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getDeclaringTypeName()).willReturn("test");
        given(methodSignature.getName()).willReturn("method");
        given(joinPoint.proceed()).willReturn(null);

        when(redisTemplate.execute(any(DefaultRedisScript.class), any(List.class), any(String.class)))
            .thenReturn(1L);

        // when
        rateLimitAspect.around(joinPoint, rateLimit);

        // then - 기본 키로 Lua 스크립트 실행
        verify(redisTemplate).execute(
            any(DefaultRedisScript.class),
            eq(Collections.singletonList("ratelimit:test.method")),
            any(String.class)
        );
    }

}
