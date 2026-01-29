package gg.agit.konect.global.auth;

import java.time.Duration;
import java.time.Instant;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccessTokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "auth:access:blacklist:";

    private final StringRedisTemplate redis;

    public void blacklist(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }

        try {
            SignedJWT jwt = SignedJWT.parse(token);
            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            String jti = claims.getJWTID();
            if (!StringUtils.hasText(jti) || claims.getExpirationTime() == null) {
                return;
            }

            Instant exp = claims.getExpirationTime().toInstant();
            Duration ttl = Duration.between(Instant.now(), exp);
            if (ttl.isNegative() || ttl.isZero()) {
                return;
            }

            redis.opsForValue().set(key(jti), "1", ttl);
        } catch (Exception ignored) {
            // ignore
        }
    }

    public boolean isBlacklisted(String jti) {
        if (!StringUtils.hasText(jti)) {
            return false;
        }
        Boolean exists = redis.hasKey(key(jti));
        return Boolean.TRUE.equals(exists);
    }

    private String key(String jti) {
        return BLACKLIST_PREFIX + jti;
    }
}
