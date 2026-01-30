package gg.agit.konect.infra.auth.oauth;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppleClientSecretProvider {

    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";
    private static final int DEFAULT_TOKEN_VALIDITY_DAYS = 180;
    private static final int DEFAULT_REFRESH_BEFORE_DAYS = 7;

    private final AppleOAuthProperties properties;
    private volatile CachedSecret cachedSecret;

    public String getClientSecret() {
        Instant now = Instant.now();
        CachedSecret current = cachedSecret;

        if (current != null && !current.shouldRefresh(now, resolveRefreshBeforeDays())) {
            return current.token();
        }

        synchronized (this) {
            current = cachedSecret;
            if (current == null || current.shouldRefresh(now, resolveRefreshBeforeDays())) {
                cachedSecret = createSecret(now);
            }
        }

        return cachedSecret.token();
    }

    private CachedSecret createSecret(Instant now) {
        int tokenValidityDays = resolveTokenValidityDays();
        String token = generateToken(now, tokenValidityDays);
        Instant expiresAt = now.plus(tokenValidityDays, ChronoUnit.DAYS);
        return new CachedSecret(token, expiresAt);
    }

    private String generateToken(Instant issuedAt, int tokenValidityDays) {
        validateRequiredProperties();
        ECPrivateKey privateKey = parsePrivateKey(readPrivateKeyFromPath(properties.getPrivateKeyPath()));

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(properties.getTeamId())
            .subject(properties.getClientId())
            .audience(APPLE_AUDIENCE)
            .issueTime(java.util.Date.from(issuedAt))
            .expirationTime(java.util.Date.from(issuedAt.plus(tokenValidityDays, ChronoUnit.DAYS)))
            .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
            .keyID(properties.getKeyId())
            .build();

        SignedJWT signedJWT = new SignedJWT(header, claims);

        try {
            signedJWT.sign(new ECDSASigner(privateKey));
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign Apple client secret.", e);
        }

        return signedJWT.serialize();
    }

    private ECPrivateKey parsePrivateKey(String rawKey) {
        String normalized = normalizePrivateKey(rawKey);
        byte[] decoded = Base64.getDecoder().decode(normalized);

        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return (ECPrivateKey)keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Apple private key.", e);
        }
    }

    private String normalizePrivateKey(String rawKey) {
        if (!StringUtils.hasText(rawKey)) {
            throw new IllegalStateException("Apple private key is missing.");
        }

        String key = rawKey.replace("\\n", "\n");
        key = key.replace("-----BEGIN PRIVATE KEY-----", "");
        key = key.replace("-----END PRIVATE KEY-----", "");
        return key.replaceAll("\\s", "");
    }

    private String readPrivateKeyFromPath(String keyPath) {
        try {
            return Files.readString(Path.of(keyPath), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read Apple private key file.", e);
        }
    }

    private void validateRequiredProperties() {
        if (!StringUtils.hasText(properties.getTeamId())) {
            throw new IllegalStateException("Apple teamId is missing.");
        }

        if (!StringUtils.hasText(properties.getClientId())) {
            throw new IllegalStateException("Apple clientId is missing.");
        }

        if (!StringUtils.hasText(properties.getKeyId())) {
            throw new IllegalStateException("Apple keyId is missing.");
        }

        if (!StringUtils.hasText(properties.getPrivateKeyPath())) {
            throw new IllegalStateException("Apple private key path is missing.");
        }
    }

    private int resolveTokenValidityDays() {
        int days = properties.getTokenValidityDays();
        return days > 0 ? days : DEFAULT_TOKEN_VALIDITY_DAYS;
    }

    private int resolveRefreshBeforeDays() {
        int days = properties.getRefreshBeforeDays();
        return days > 0 ? days : DEFAULT_REFRESH_BEFORE_DAYS;
    }

    private record CachedSecret(String token, Instant expiresAt) {
        private boolean shouldRefresh(Instant now, int refreshBeforeDays) {
            Instant refreshAt = expiresAt.minus(refreshBeforeDays, ChronoUnit.DAYS);
            return now.isAfter(refreshAt);
        }
    }
}
