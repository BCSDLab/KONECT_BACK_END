package gg.agit.konect.global.lock.aspect;

import java.time.Duration;
import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.global.lock.annotation.PreventDuplicate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class PreventDuplicateAspect {

    private static final String KEY_PREFIX = "dedupe:";
    private static final String KEY_VALUE = "1";

    private final StringRedisTemplate redis;
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Around("@annotation(preventDuplicate)")
    public Object preventDuplicate(ProceedingJoinPoint joinPoint, PreventDuplicate preventDuplicate) throws Throwable {
        String evaluatedKey = evaluateSpel(preventDuplicate.key(), joinPoint);
        if (!StringUtils.hasText(evaluatedKey)) {
            log.warn("SpEL 파싱 실패 또는 null, 중복 방지 없이 진행: expression={}", preventDuplicate.key());
            return joinPoint.proceed();
        }

        Duration ttl = toDuration(preventDuplicate);
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            log.warn("유효하지 않은 ttl, 중복 방지 없이 진행: expression={}, ttl={}", preventDuplicate.key(), ttl);
            return joinPoint.proceed();
        }

        // 키 충돌 방지: 메서드 단위 네임스페이스 포함
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodNamespace = signature.getDeclaringTypeName() + "#" + signature.getName();

        String dedupeKey = KEY_PREFIX + methodNamespace + ":" + evaluatedKey;

        try {
            Boolean acquired = redis.opsForValue().setIfAbsent(dedupeKey, KEY_VALUE, ttl);

            if (Boolean.TRUE.equals(acquired)) {
                log.debug("중복 방지 키 생성 성공: key={}, ttl={}ms", dedupeKey, ttl.toMillis());
                return joinPoint.proceed();
            }

            log.info("중복 요청 차단: key={}", dedupeKey);
            throw CustomException.of(ApiResponseCode.DUPLICATE_REQUEST);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis 연결 실패, 중복 방지 없이 진행: key={}", dedupeKey);
            log.debug("Redis failure detail", e);
            return joinPoint.proceed();
        }
    }

    private Duration toDuration(PreventDuplicate preventDuplicate) {
        try {
            return Duration.ofMillis(preventDuplicate.timeUnit().toMillis(preventDuplicate.leaseTime()));
        } catch (Exception e) {
            log.warn("ttl 변환 실패, 중복 방지 없이 진행: leaseTime={}, timeUnit={}",
                preventDuplicate.leaseTime(), preventDuplicate.timeUnit());
            log.debug("Duration conversion failure detail", e);
            return null;
        }
    }

    private String evaluateSpel(String expression, ProceedingJoinPoint joinPoint) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }

        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setRootObject(joinPoint.getTarget());
        context.setVariable("args", args);
        context.setVariable("target", joinPoint.getTarget());

        for (int i = 0; i < args.length; i++) {
            if (parameterNames != null && i < parameterNames.length && StringUtils.hasText(parameterNames[i])) {
                context.setVariable(parameterNames[i], args[i]);
            }
            context.setVariable("p" + i, args[i]);
            context.setVariable("a" + i, args[i]);
            context.setVariable("arg" + i, args[i]);
        }

        try {
            Expression parsed = expressionParser.parseExpression(expression);
            Object value = parsed.getValue(context);
            if (value == null) {
                return null;
            }
            String evaluated = Objects.toString(value, null);
            if (!StringUtils.hasText(evaluated)) {
                return null;
            }
            return evaluated;
        } catch (Exception e) {
            log.warn("SpEL 파싱 실패 또는 null, 중복 방지 없이 진행: expression={}", expression);
            log.debug("SpEL evaluation failure detail", e);
            return null;
        }
    }
}
