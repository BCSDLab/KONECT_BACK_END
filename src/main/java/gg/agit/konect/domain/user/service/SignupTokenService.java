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

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignupTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final String KEY_PREFIX = "auth:signup:";
    private static final String DELIMITER = "|";
    private static final int EXPECTED_PARTS = 3;

    private static final DefaultRedisScript<String> GET_DEL_SCRIPT =
        new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]); " +
                "if v then redis.call('DEL', KEYS[1]); end; " +
                "return v;",
            String.class
        );

    private final SecureRandom secureRandom = new SecureRandom();

    private final StringRedisTemplate redis;

    @Value("${app.auth.signup-token-ttl-seconds:600}")
    private long signupTokenTtlSeconds;

    public Duration signupTtl() {
        if (signupTokenTtlSeconds <= 0) {
            throw new IllegalStateException("app.auth.signup-token-ttl-seconds must be positive");
        }
        return Duration.ofSeconds(signupTokenTtlSeconds);
    }

    public String issue(String email, Provider provider, String providerId) {
        if (!StringUtils.hasText(email) || provider == null) {
            throw new IllegalArgumentException("email and provider are required");
        }

        String token = generateToken();
        redis.opsForValue().set(key(token), serialize(new SignupClaims(email, provider, providerId)), signupTtl());
        return token;
    }

    public SignupClaims consumeOrThrow(String token) {
        if (!StringUtils.hasText(token)) {
            throw CustomException.of(ApiResponseCode.INVALID_SESSION);
        }

        String value = redis.execute(GET_DEL_SCRIPT, List.of(key(token)));
        SignupClaims claims = deserialize(value);
        if (claims == null) {
            throw CustomException.of(ApiResponseCode.INVALID_SESSION);
        }
        return claims;
    }

    private String key(String token) {
        return KEY_PREFIX + token;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String serialize(SignupClaims claims) {
        String safeProviderId = claims.providerId() == null ? "" : claims.providerId();
        return claims.email() + DELIMITER + claims.provider().name() + DELIMITER + safeProviderId;
    }

    private SignupClaims deserialize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String[] parts = value.split("\\|", -1);
        if (parts.length != EXPECTED_PARTS) {
            return null;
        }

        String email = parts[0];
        String provider = parts[1];
        String providerId = parts[2];

        if (!StringUtils.hasText(email) || !StringUtils.hasText(provider)) {
            return null;
        }

        try {
            Provider p = Provider.valueOf(provider);
            return new SignupClaims(email, p, StringUtils.hasText(providerId) ? providerId : null);
        } catch (Exception e) {
            return null;
        }
    }

    public record SignupClaims(String email, Provider provider, String providerId) {
    }
}
