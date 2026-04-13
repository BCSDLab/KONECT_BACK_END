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
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
@Aspect
@Component
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

    // Lua 스크립트를 재사용하기 위해 미리 컴파일 (매 요청마다 생성 비용 제거)
    private final DefaultRedisScript<Long> incrWithTtlScript;

    public RateLimitAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.incrWithTtlScript = new DefaultRedisScript<>(INCR_WITH_TTL_SCRIPT, Long.class);
    }

    @Around("@within(rateLimit) || @annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = generateKey(joinPoint, rateLimit);
        int maxRequests = rateLimit.maxRequests();
        int timeWindowSeconds = rateLimit.timeWindowSeconds();

        // Redis 장애 시 fail-open 정책: 예외 발생하면 rate limit 체크를 스킵하고 요청 처리
        long currentCount;
        try {
            // 미리 생성해둔 Lua 스크립트 실행 (원자적 INCR + TTL 설정)
            Long count = redisTemplate.execute(
                incrWithTtlScript,
                Collections.singletonList(key),
                String.valueOf(timeWindowSeconds)
            );
            currentCount = count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Rate limiting Redis operation failed for key={}: {}. Skipping rate limit check.",
                key, e.getMessage());
            return joinPoint.proceed();
        }

        // 제한 초과 확인 - 초과 시에만 TTL 조회
        if (currentCount > maxRequests) {
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
            log.error("SpEL expression evaluation failed for keyExpression='{}', methodKey='{}': {}",
                keyExpression, methodKey, e.getMessage());
            return RATE_LIMIT_KEY_PREFIX + methodKey;
        }
    }
}
