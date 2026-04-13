package gg.agit.konect.global.ratelimit.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import gg.agit.konect.global.ratelimit.annotation.RateLimit;
import gg.agit.konect.global.ratelimit.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:";

    // Lua 스크립트: INCR로 원자적 증가 후, 처음 생성된 경우에만 TTL 설정
    // 이 방식으로 SETNX와 INCR 사이의 레이스 컨디션을 방지
    private static final String INCR_WITH_TTL_SCRIPT =
        "local current = redis.call('INCR', KEYS[1]) " +
            "if current == 1 then " +
            "    redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
            "end " +
            "return current";

    private final StringRedisTemplate redisTemplate;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = generateKey(joinPoint, rateLimit);
        int maxRequests = rateLimit.maxRequests();
        int timeWindowSeconds = rateLimit.timeWindowSeconds();

        // Lua 스크립트로 원자적 연산: INCR + 첫 생성 시 TTL 설정
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(INCR_WITH_TTL_SCRIPT, Long.class);
        Long currentCount = redisTemplate.execute(
            script,
            Collections.singletonList(key),
            String.valueOf(timeWindowSeconds)
        );

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
