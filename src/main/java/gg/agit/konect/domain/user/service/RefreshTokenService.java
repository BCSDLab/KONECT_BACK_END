package gg.agit.konect.domain.user.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import gg.agit.konect.global.auth.jwt.JwtProperties;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);
    private static final int MIN_HS256_SECRET_BYTES = 32;
    private static final String CLAIM_USER_ID = "id";
    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties jwtProperties;

    public Duration refreshTtl() {
        return REFRESH_TOKEN_TTL;
    }

    public String issue(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(refreshTtl());

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(resolveIssuer())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expiresAt))
            .jwtID(UUID.randomUUID().toString())
            .claim(CLAIM_USER_ID, userId)
            .claim(CLAIM_TOKEN_TYPE, REFRESH_TOKEN_TYPE)
            .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);

        try {
            jwt.sign(new MACSigner(resolveSecretBytes()));
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign refresh token.", e);
        }

        return jwt.serialize();
    }

    public Rotated rotate(String refreshToken) {
        Integer userId = validateAndExtractUserId(refreshToken);

        String newToken = issue(userId);
        return new Rotated(userId, newToken);
    }

    private Integer validateAndExtractUserId(String refreshToken) {
        log.info("refreshToken: {}", refreshToken);
        if (!StringUtils.hasText(refreshToken)) {
            throw CustomException.of(ApiResponseCode.INVALID_REFRESH_TOKEN);
        }

        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(refreshToken);
        } catch (Exception e) {
            throw CustomException.of(ApiResponseCode.INVALID_REFRESH_TOKEN);
        }

        try {
            if (!jwt.verify(new MACVerifier(resolveSecretBytes()))) {
                throw CustomException.of(ApiResponseCode.INVALID_REFRESH_TOKEN);
            }
        } catch (JOSEException e) {
            throw CustomException.of(ApiResponseCode.INVALID_REFRESH_TOKEN);
        }

        JWTClaimsSet claims;
        try {
            claims = jwt.getJWTClaimsSet();
        } catch (Exception e) {
            throw CustomException.of(ApiResponseCode.INVALID_REFRESH_TOKEN);
        }

        if (!resolveIssuer().equals(claims.getIssuer())) {
            throw CustomException.of(ApiResponseCode.INVALID_REFRESH_TOKEN);
        }

        Date exp = claims.getExpirationTime();
        if (exp == null || Instant.now().isAfter(exp.toInstant())) {
            throw CustomException.of(ApiResponseCode.INVALID_REFRESH_TOKEN);
        }

        Object tokenType = claims.getClaim(CLAIM_TOKEN_TYPE);
        if (!(tokenType instanceof String tokenTypeValue) || !REFRESH_TOKEN_TYPE.equals(tokenTypeValue)) {
            throw CustomException.of(ApiResponseCode.INVALID_REFRESH_TOKEN);
        }

        Object id = claims.getClaim(CLAIM_USER_ID);
        if (!(id instanceof Number number)) {
            throw CustomException.of(ApiResponseCode.INVALID_REFRESH_TOKEN);
        }

        return number.intValue();
    }

    private String resolveIssuer() {
        String issuer = jwtProperties.issuer();
        if (!StringUtils.hasText(issuer)) {
            throw new IllegalStateException("app.jwt.issuer is required");
        }

        return issuer;
    }

    private byte[] resolveSecretBytes() {
        String secret = jwtProperties.secret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("app.jwt.secret is required");
        }

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_HS256_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
        }

        return bytes;
    }

    public record Rotated(Integer userId, String refreshToken) {
    }
}
