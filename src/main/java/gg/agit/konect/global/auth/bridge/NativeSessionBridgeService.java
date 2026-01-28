package gg.agit.konect.global.auth.bridge;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NativeSessionBridgeService {

    private static final int TOKEN_BYTES = 32;
    private static final String KEY_PREFIX = "native:session-bridge:";
    private static final Duration TTL = Duration.ofSeconds(30);

    private final SecureRandom secureRandom = new SecureRandom();

    private final StringRedisTemplate redis;

    public String issue(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        String token = generateToken();
        redis.opsForValue().set(KEY_PREFIX + token, userId.toString(), TTL);

        return token;
    }

    public Optional<Integer> consume(@Nullable String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        if (redis == null) {
            throw new IllegalStateException("Redis is required for native session bridge token storage.");
        }

        String value = redis.opsForValue().getAndDelete(KEY_PREFIX + token);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
