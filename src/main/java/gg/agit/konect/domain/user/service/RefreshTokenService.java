package gg.agit.konect.domain.user.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private static final String ACTIVE_PREFIX = "auth:refresh:active:";
    private static final String REVOKED_PREFIX = "auth:refresh:revoked:";
    private static final String USER_SET_PREFIX = "auth:refresh:user:";

    private static final DefaultRedisScript<String> GET_DEL_SCRIPT =
        new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]); " +
                "if v then redis.call('DEL', KEYS[1]); end; " +
                "return v;",
            String.class
        );

    private final SecureRandom secureRandom = new SecureRandom();

    private final StringRedisTemplate redis;

    @Value("${app.auth.refresh-token-ttl-seconds:2592000}")
    private long refreshTokenTtlSeconds;

    public Duration refreshTtl() {
        if (refreshTokenTtlSeconds <= 0) {
            throw new IllegalStateException("app.auth.refresh-token-ttl-seconds must be positive");
        }
        return Duration.ofSeconds(refreshTokenTtlSeconds);
    }

    public String issue(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        String token = generateToken();
        Duration ttl = refreshTtl();

        redis.opsForValue().set(activeKey(token), userId.toString(), ttl);
        redis.opsForSet().add(userSetKey(userId), token);
        redis.expire(userSetKey(userId), ttl);

        return token;
    }

    public Rotated rotate(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw CustomException.of(ApiResponseCode.INVALID_SESSION);
        }

        Integer userId = consumeActive(refreshToken);
        if (userId == null) {
            Integer revokedUserId = findRevokedUserId(refreshToken);
            if (revokedUserId != null) {
                revokeAll(revokedUserId);
            }
            throw CustomException.of(ApiResponseCode.INVALID_SESSION);
        }

        String newToken = issue(userId);
        return new Rotated(userId, newToken);
    }

    public void revoke(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }

        String value = redis.execute(GET_DEL_SCRIPT, List.of(activeKey(refreshToken)));
        Integer userId = parseUserId(value);
        if (userId == null) {
            return;
        }

        Duration ttl = refreshTtl();
        redis.opsForValue().set(revokedKey(refreshToken), userId.toString(), ttl);
        redis.opsForSet().remove(userSetKey(userId), refreshToken);
    }

    public void revokeAll(Integer userId) {
        if (userId == null) {
            return;
        }

        String setKey = userSetKey(userId);
        var tokens = redis.opsForSet().members(setKey);

        if (tokens == null || tokens.isEmpty()) {
            redis.delete(setKey);
            return;
        }

        Duration ttl = refreshTtl();
        for (String token : tokens) {
            redis.delete(activeKey(token));
            redis.opsForValue().set(revokedKey(token), userId.toString(), ttl);
        }

        redis.delete(setKey);
    }

    private Integer consumeActive(String token) {
        String value = redis.execute(GET_DEL_SCRIPT, List.of(activeKey(token)));
        Integer userId = parseUserId(value);
        if (userId == null) {
            return null;
        }

        Duration ttl = refreshTtl();
        redis.opsForValue().set(revokedKey(token), userId.toString(), ttl);
        redis.opsForSet().remove(userSetKey(userId), token);
        return userId;
    }

    private Integer findRevokedUserId(String token) {
        String value = redis.opsForValue().get(revokedKey(token));
        return parseUserId(value);
    }

    private String activeKey(String token) {
        return ACTIVE_PREFIX + token;
    }

    private String revokedKey(String token) {
        return REVOKED_PREFIX + token;
    }

    private String userSetKey(Integer userId) {
        return USER_SET_PREFIX + userId;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Integer parseUserId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public record Rotated(Integer userId, String refreshToken) {
    }
}
