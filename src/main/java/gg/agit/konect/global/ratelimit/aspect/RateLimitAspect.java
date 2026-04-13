package gg.agit.konect.global.ratelimit.aspect;

import java.time.Duration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import gg.agit.konect.global.ratelimit.annotation.RateLimit;
import gg.agit.konect.global.ratelimit.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = generateKey(joinPoint, rateLimit);
        int maxRequests = rateLimit.maxRequests();
        int timeWindowSeconds = rateLimit.timeWindowSeconds();

        // 키가 없으면 TTL과 함께 초기화 (원자적 연산)
        redisTemplate.opsForValue().setIfAbsent(
            key, "0", Duration.ofSeconds(timeWindowSeconds)
        );

        // 요청 횟수 증가
        Long currentCount = redisTemplate.opsForValue().increment(key);

        // 제한 초과 확인
        if (currentCount != null && currentCount > maxRequests) {
            // 남은 TTL 조회 (초 단위)
            Long remainingSeconds = redisTemplate.getExpire(key);
            long remaining = remainingSeconds != null && remainingSeconds > 0
                ? remainingSeconds
                : timeWindowSeconds;

            throw RateLimitExceededException.withRemainingTime(remaining);
        }

        return joinPoint.proceed();
    }

    private String generateKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String keyExpression = rateLimit.keyExpression();
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        String methodKey = signature.getDeclaringTypeName() + "." + signature.getName();

        if (!StringUtils.hasText(keyExpression)) {
            return RATE_LIMIT_KEY_PREFIX + methodKey;
        }

        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        try {
            Object result = parser.parseExpression(keyExpression).getValue(context);
            String keyValue = result != null ? result.toString() : "unknown";
            return RATE_LIMIT_KEY_PREFIX + methodKey + ":" + keyValue;
        } catch (Exception e) {
            return RATE_LIMIT_KEY_PREFIX + methodKey;
        }
    }
}
