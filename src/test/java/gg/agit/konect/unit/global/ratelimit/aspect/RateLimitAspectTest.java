package gg.agit.konect.unit.global.ratelimit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.global.ratelimit.annotation.RateLimit;
import gg.agit.konect.global.ratelimit.aspect.RateLimitAspect;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private RateLimit rateLimit;

    @InjectMocks
    private RateLimitAspect rateLimitAspect;

    @Test
    @DisplayName("제한 내 요청 시 정상 통과")
    void allowsRequestWithinLimit() throws Throwable {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(
            "ratelimit:test.method:user123", "0", Duration.ofSeconds(60)
        )).willReturn(true);
        given(valueOperations.increment("ratelimit:test.method:user123")).willReturn(5L);
        given(rateLimit.maxRequests()).willReturn(10);
        given(rateLimit.timeWindowSeconds()).willReturn(60);
        given(rateLimit.keyExpression()).willReturn("#userId");
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getDeclaringTypeName()).willReturn("test");
        given(methodSignature.getName()).willReturn("method");
        given(methodSignature.getParameterNames()).willReturn(new String[]{"userId"});
        given(joinPoint.getArgs()).willReturn(new Object[]{"user123"});

        Object expectedResult = new Object();
        given(joinPoint.proceed()).willReturn(expectedResult);

        // when
        Object result = rateLimitAspect.around(joinPoint, rateLimit);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    @DisplayName("첫 번째 요청 시 setIfAbsent로 TTL 설정")
    void setsTtlOnFirstRequestWithSetIfAbsent() throws Throwable {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(
            "ratelimit:test.method:user123", "0", Duration.ofSeconds(60)
        )).willReturn(true);
        given(valueOperations.increment("ratelimit:test.method:user123")).willReturn(1L);
        given(rateLimit.maxRequests()).willReturn(10);
        given(rateLimit.timeWindowSeconds()).willReturn(60);
        given(rateLimit.keyExpression()).willReturn("#userId");
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getDeclaringTypeName()).willReturn("test");
        given(methodSignature.getName()).willReturn("method");
        given(methodSignature.getParameterNames()).willReturn(new String[]{"userId"});
        given(joinPoint.getArgs()).willReturn(new Object[]{"user123"});
        given(joinPoint.proceed()).willReturn(null);

        // when
        rateLimitAspect.around(joinPoint, rateLimit);

        // then
        verify(valueOperations).setIfAbsent(
            "ratelimit:test.method:user123", "0", Duration.ofSeconds(60)
        );
    }

    @Test
    @DisplayName("제한 초과 시 RateLimitExceededException 발생")
    void throwsExceptionWhenLimitExceeded() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(
            "ratelimit:test.method:user123", "0", Duration.ofSeconds(60)
        )).willReturn(false);
        given(valueOperations.increment("ratelimit:test.method:user123")).willReturn(11L);
        given(rateLimit.maxRequests()).willReturn(10);
        given(rateLimit.timeWindowSeconds()).willReturn(60);
        given(rateLimit.keyExpression()).willReturn("#userId");
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getDeclaringTypeName()).willReturn("test");
        given(methodSignature.getName()).willReturn("method");
        given(methodSignature.getParameterNames()).willReturn(new String[]{"userId"});
        given(joinPoint.getArgs()).willReturn(new Object[]{"user123"});
        given(redisTemplate.getExpire("ratelimit:test.method:user123")).willReturn(30L);

        // when & then
        assertThatThrownBy(() -> rateLimitAspect.around(joinPoint, rateLimit))
            .isInstanceOf(CustomException.class)
            .satisfies(ex -> {
                CustomException customEx = (CustomException)ex;
                assertThat(customEx.getErrorCode()).isEqualTo(ApiResponseCode.TOO_MANY_REQUESTS);
            });
    }

    @Test
    @DisplayName("빈 keyExpression 시 메서드 시그니처 기본 키 사용")
    void usesDefaultKeyWhenExpressionIsEmpty() throws Throwable {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(
            "ratelimit:test.method", "0", Duration.ofSeconds(60)
        )).willReturn(true);
        given(valueOperations.increment("ratelimit:test.method")).willReturn(1L);
        given(rateLimit.maxRequests()).willReturn(10);
        given(rateLimit.timeWindowSeconds()).willReturn(60);
        given(rateLimit.keyExpression()).willReturn("");
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getDeclaringTypeName()).willReturn("test");
        given(methodSignature.getName()).willReturn("method");
        given(joinPoint.proceed()).willReturn(null);

        // when
        rateLimitAspect.around(joinPoint, rateLimit);

        // then
        verify(valueOperations).setIfAbsent(
            "ratelimit:test.method", "0", Duration.ofSeconds(60)
        );
    }

}
