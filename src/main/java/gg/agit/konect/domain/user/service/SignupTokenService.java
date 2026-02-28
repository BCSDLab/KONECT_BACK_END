package gg.agit.konect.domain.user.service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.global.auth.util.SecureTokenGenerator;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignupTokenService {

    private static final Duration SIGNUP_TOKEN_TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "auth:signup:";
    private static final String DELIMITER = "|";
    private static final int INDEX_PROVIDER_ID = 2;
    private static final int INDEX_NAME = 3;
    private static final int EXPECTED_PARTS_WITHOUT_NAME = 3;
    private static final int EXPECTED_PARTS_WITH_NAME = 4;

    private static final DefaultRedisScript<String> GET_DEL_SCRIPT =
        new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]); " +
                "if v then redis.call('DEL', KEYS[1]); end; " +
                "return v;",
            String.class
        );

    private final StringRedisTemplate redis;
    private final SecureTokenGenerator secureTokenGenerator;

    public Duration signupTtl() {
        return SIGNUP_TOKEN_TTL;
    }

    public String issue(String email, Provider provider, String providerId) {
        return issue(email, provider, providerId, null);
    }

    public String issue(String email, Provider provider, String providerId, String name) {
        if (!StringUtils.hasText(email) || provider == null) {
            throw new IllegalArgumentException("email and provider are required");
        }

        String token = secureTokenGenerator.generate();
        SignupClaims claims = new SignupClaims(email, provider, providerId, name);
        redis.opsForValue().set(key(token), serialize(claims), signupTtl());
        return token;
    }

    public SignupClaims readOrThrow(String token) {
        if (!StringUtils.hasText(token)) {
            throw CustomException.of(ApiResponseCode.INVALID_SIGNUP_TOKEN);
        }

        String value = redis.opsForValue().get(key(token));
        SignupClaims claims = deserialize(value);

        if (claims == null) {
            throw CustomException.of(ApiResponseCode.INVALID_SIGNUP_TOKEN);
        }

        return claims;
    }

    public SignupClaims consumeOrThrow(String token) {
        if (!StringUtils.hasText(token)) {
            throw CustomException.of(ApiResponseCode.INVALID_SIGNUP_TOKEN);
        }

        String value = redis.execute(GET_DEL_SCRIPT, List.of(key(token)));
        SignupClaims claims = deserialize(value);
        if (claims == null) {
            throw CustomException.of(ApiResponseCode.INVALID_SIGNUP_TOKEN);
        }
        return claims;
    }

    private String key(String token) {
        return KEY_PREFIX + token;
    }

    private String serialize(SignupClaims claims) {
        String safeProviderId = claims.providerId() == null ? "" : claims.providerId();
        String safeName = claims.name() == null ? "" : claims.name();
        return claims.email() + DELIMITER + claims.provider().name() + DELIMITER
            + safeProviderId + DELIMITER + safeName;
    }

    private SignupClaims deserialize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String[] parts = value.split("\\|", -1);
        if (parts.length != EXPECTED_PARTS_WITHOUT_NAME && parts.length != EXPECTED_PARTS_WITH_NAME) {
            return null;
        }

        String email = parts[0];
        String provider = parts[1];
        String providerId = parts[INDEX_PROVIDER_ID];
        String name = parts.length == EXPECTED_PARTS_WITH_NAME ? parts[INDEX_NAME] : null;

        if (!StringUtils.hasText(email) || !StringUtils.hasText(provider)) {
            return null;
        }

        try {
            Provider p = Provider.valueOf(provider);
            return new SignupClaims(
                email,
                p,
                StringUtils.hasText(providerId) ? providerId : null,
                StringUtils.hasText(name) ? name : null
            );
        } catch (Exception e) {
            return null;
        }
    }

    public record SignupClaims(String email, Provider provider, String providerId, String name) {
    }
}
