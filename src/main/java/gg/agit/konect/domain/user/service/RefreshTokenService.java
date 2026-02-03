package gg.agit.konect.domain.user.service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import gg.agit.konect.global.auth.util.SecureTokenGenerator;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private static final String ACTIVE_PREFIX = "auth:refresh:active:";
    private static final String USER_SET_PREFIX = "auth:refresh:user:";

    private static final DefaultRedisScript<String> GET_DEL_SCRIPT =
        new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]); " +
                "if v then redis.call('DEL', KEYS[1]); end; " +
                "return v;",
            String.class
        );

    private final StringRedisTemplate redis;
    private final SecureTokenGenerator secureTokenGenerator;

    public Duration refreshTtl() {
        return REFRESH_TOKEN_TTL;
    }

    public String issue(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        String token = secureTokenGenerator.generate();
        Duration ttl = refreshTtl();

        redis.opsForValue().set(activeKey(token), userId.toString(), ttl);
        redis.opsForSet().add(userSetKey(userId), token);
        redis.expire(userSetKey(userId), ttl);

        return token;
    }

    public Rotated rotate(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw CustomException.of(ApiResponseCode.INVALID_REFRESH_TOKEN);
        }

        Integer userId = consumeActive(refreshToken);
        if (userId == null) {
            throw CustomException.of(ApiResponseCode.INVALID_REFRESH_TOKEN);
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

        for (String token : tokens) {
            redis.delete(activeKey(token));
        }

        redis.delete(setKey);
    }

    private Integer consumeActive(String token) {
        String value = redis.execute(GET_DEL_SCRIPT, List.of(activeKey(token)));
        Integer userId = parseUserId(value);
        if (userId == null) {
            return null;
        }

        redis.opsForSet().remove(userSetKey(userId), token);
        return userId;
    }

    private String activeKey(String token) {
        return ACTIVE_PREFIX + token;
    }

    private String userSetKey(Integer userId) {
        return USER_SET_PREFIX + userId;
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
