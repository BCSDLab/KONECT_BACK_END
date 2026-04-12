package gg.agit.konect.global.auth.oauth;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import gg.agit.konect.global.auth.util.SecureTokenGenerator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NativeSessionBridgeService {

    private static final String KEY_PREFIX = "native:session-bridge:";
    private static final Duration TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redis;
    private final SecureTokenGenerator secureTokenGenerator;

    public String issue(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        String token = secureTokenGenerator.generate();
        redis.opsForValue().set(KEY_PREFIX + token, userId.toString(), TTL);

        return token;
    }

    public Optional<Integer> consume(@Nullable String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String key = KEY_PREFIX + token;
        String value = redis.opsForValue().getAndDelete(key);

        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
