package gg.agit.konect.global.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private static final int MIN_HS256_SECRET_BYTES = 32;
    private static final String CLAIM_USER_ID = "id";
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    private final JwtProperties properties;
    private final AccessTokenBlacklistService accessTokenBlacklistService;

    public String createToken(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(ACCESS_TOKEN_TTL);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(resolveIssuer())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expiresAt))
            .jwtID(UUID.randomUUID().toString())
            .claim(CLAIM_USER_ID, userId)
            .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);

        try {
            jwt.sign(new MACSigner(resolveSecretBytes()));
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign access token.", e);
        }

        return jwt.serialize();
    }

    public Integer getUserId(String token) {
        if (!StringUtils.hasText(token)) {
            throw CustomException.of(ApiResponseCode.MISSING_ACCESS_TOKEN);
        }

        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(token);
        } catch (Exception e) {
            throw CustomException.of(ApiResponseCode.MALFORMED_ACCESS_TOKEN);
        }

        try {
            if (!jwt.verify(new MACVerifier(resolveSecretBytes()))) {
                throw CustomException.of(ApiResponseCode.INVALID_ACCESS_TOKEN_SIGNATURE);
            }
        } catch (JOSEException e) {
            throw CustomException.of(ApiResponseCode.INVALID_ACCESS_TOKEN_SIGNATURE);
        }

        JWTClaimsSet claims;
        try {
            claims = jwt.getJWTClaimsSet();
        } catch (Exception e) {
            throw CustomException.of(ApiResponseCode.INVALID_ACCESS_TOKEN_CLAIMS);
        }

        if (!resolveIssuer().equals(claims.getIssuer())) {
            throw CustomException.of(ApiResponseCode.INVALID_ACCESS_TOKEN_ISSUER);
        }

        Date exp = claims.getExpirationTime();
        if (exp == null) {
            throw CustomException.of(ApiResponseCode.INVALID_ACCESS_TOKEN_CLAIMS);
        }

        if (Instant.now().isAfter(exp.toInstant())) {
            throw CustomException.of(ApiResponseCode.EXPIRED_TOKEN);
        }

        String jti = claims.getJWTID();
        if (!StringUtils.hasText(jti)) {
            throw CustomException.of(ApiResponseCode.INVALID_ACCESS_TOKEN_CLAIMS);
        }

        if (accessTokenBlacklistService.isBlacklisted(jti)) {
            throw CustomException.of(ApiResponseCode.BLACKLISTED_ACCESS_TOKEN);
        }

        Object id = claims.getClaim(CLAIM_USER_ID);
        if (!(id instanceof Number number)) {
            throw CustomException.of(ApiResponseCode.INVALID_ACCESS_TOKEN_CLAIMS);
        }

        return number.intValue();
    }

    private String resolveIssuer() {
        String issuer = properties.issuer();
        if (!StringUtils.hasText(issuer)) {
            throw new IllegalStateException("app.jwt.issuer is required");
        }
        return issuer;
    }

    private byte[] resolveSecretBytes() {
        String secret = properties.secret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("app.jwt.secret is required");
        }

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_HS256_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
        }
        return bytes;
    }
}
