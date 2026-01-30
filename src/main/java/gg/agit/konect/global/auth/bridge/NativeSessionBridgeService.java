package gg.agit.konect.global.auth.bridge;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NativeSessionBridgeService {

    private static final int TOKEN_BYTES = 32;
    private static final String KEY_PREFIX = "native:session-bridge:";
    private static final Duration TTL = Duration.ofSeconds(30);
    private static final DefaultRedisScript<String> GET_DEL_SCRIPT =
        new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]); " +
                "if v then redis.call('DEL', KEYS[1]); end; " +
                "return v;",
            String.class
        );

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

        String key = KEY_PREFIX + token;
        String value = redis.execute(GET_DEL_SCRIPT, List.of(key));

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
