package gg.agit.konect.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import gg.agit.konect.global.auth.jwt.JwtProperties;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;

class RefreshTokenServiceTest extends ServiceTestSupport {

    private static final String VALID_SECRET = "0123456789abcdef0123456789abcdef";
    private static final String VALID_ISSUER = "konect";

    private final RefreshTokenService refreshTokenService = new RefreshTokenService(
        new JwtProperties(VALID_SECRET, VALID_ISSUER)
    );

    @Test
    @DisplayName("issue와 extractUserId는 정상 리프레시 토큰을 왕복 처리한다")
    void issueAndExtractUserIdRoundTrip() {
        // when
        String token = refreshTokenService.issue(123);

        // then
        assertThat(refreshTokenService.extractUserId(token)).isEqualTo(123);
    }

    @Test
    @DisplayName("rotate는 같은 사용자 ID를 유지하면서 새 토큰을 발급한다")
    void rotateIssuesNewTokenForSameUser() {
        // given
        String originalToken = refreshTokenService.issue(7);

        // when
        RefreshTokenService.Rotated rotated = refreshTokenService.rotate(originalToken);

        // then
        assertThat(rotated.userId()).isEqualTo(7);
        assertThat(rotated.refreshToken()).isNotBlank();
        assertThat(rotated.refreshToken()).isNotEqualTo(originalToken);
        assertThat(refreshTokenService.extractUserId(rotated.refreshToken())).isEqualTo(7);
    }

    @Test
    @DisplayName("extractUserId는 빈 토큰을 거부한다")
    void extractUserIdRejectsBlankToken() {
        assertInvalidRefreshToken(() -> refreshTokenService.extractUserId(" "));
    }

    @Test
    @DisplayName("extractUserId는 서명이 다른 토큰을 거부한다")
    void extractUserIdRejectsTokenSignedWithDifferentSecret() {
        // given
        RefreshTokenService otherService = new RefreshTokenService(
            new JwtProperties("fedcba9876543210fedcba9876543210", VALID_ISSUER)
        );
        String token = otherService.issue(9);

        // when & then
        assertInvalidRefreshToken(() -> refreshTokenService.extractUserId(token));
    }

    @Test
    @DisplayName("extractUserId는 만료된 토큰을 거부한다")
    void extractUserIdRejectsExpiredToken() throws JOSEException {
        // given
        String token = createToken(11, VALID_ISSUER, "refresh", Instant.now().minusSeconds(5), VALID_SECRET);

        // when & then
        assertInvalidRefreshToken(() -> refreshTokenService.extractUserId(token));
    }

    @Test
    @DisplayName("extractUserId는 refresh 타입이 아니면 거부한다")
    void extractUserIdRejectsNonRefreshTokenType() throws JOSEException {
        // given
        String token = createToken(11, VALID_ISSUER, "access", Instant.now().plusSeconds(60), VALID_SECRET);

        // when & then
        assertInvalidRefreshToken(() -> refreshTokenService.extractUserId(token));
    }

    @Test
    @DisplayName("issue는 userId가 없으면 IllegalArgumentException을 던진다")
    void issueRejectsNullUserId() {
        assertThatThrownBy(() -> refreshTokenService.issue(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("userId is required");
    }

    @Test
    @DisplayName("issue는 issuer 설정이 비어 있으면 실패한다")
    void issueFailsWhenIssuerMissing() {
        RefreshTokenService service = new RefreshTokenService(new JwtProperties(VALID_SECRET, " "));

        assertThatThrownBy(() -> service.issue(1))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("app.jwt.issuer is required");
    }

    @Test
    @DisplayName("issue는 secret 길이가 32바이트보다 짧으면 실패한다")
    void issueFailsWhenSecretTooShort() {
        RefreshTokenService service = new RefreshTokenService(new JwtProperties("short-secret", VALID_ISSUER));

        assertThatThrownBy(() -> service.issue(1))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("app.jwt.secret must be at least 32 bytes");
    }

    private String createToken(Integer userId, String issuer, String tokenType, Instant expiresAt, String secret)
        throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .issueTime(Date.from(Instant.now().minusSeconds(10)))
            .expirationTime(Date.from(expiresAt))
            .jwtID(UUID.randomUUID().toString())
            .claim("id", userId)
            .claim("token_type", tokenType)
            .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret));
        return jwt.serialize();
    }

    private void assertInvalidRefreshToken(ThrowingCallable callable) {
        assertThatThrownBy(callable::call)
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode())
                .isEqualTo(ApiResponseCode.INVALID_REFRESH_TOKEN));
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call() throws Exception;
    }
}
