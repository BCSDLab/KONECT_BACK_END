package gg.agit.konect.unit.domain.user.service;

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

import gg.agit.konect.domain.user.service.RefreshTokenService;
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

    @Test
    @DisplayName("issue는 userId가 0이어도 토큰을 발급한다")
    void issueAcceptsZeroUserId() {
        // when
        String token = refreshTokenService.issue(0);

        // then
        assertThat(refreshTokenService.extractUserId(token)).isEqualTo(0);
    }

    @Test
    @DisplayName("extractUserId는 빈 문자열을 거부한다")
    void extractUserIdRejectsEmptyString() {
        assertInvalidRefreshToken(() -> refreshTokenService.extractUserId(""));
    }

    @Test
    @DisplayName("extractUserId는 null을 거부한다")
    void extractUserIdRejectsNull() {
        assertInvalidRefreshToken(() -> refreshTokenService.extractUserId(null));
    }

    @Test
    @DisplayName("extractUserId는 탭/개행 문자열을 거부한다")
    void extractUserIdRejectsTabNewlineString() {
        assertInvalidRefreshToken(() -> refreshTokenService.extractUserId("\t\n"));
    }

    @Test
    @DisplayName("extractUserId는 잘린/손상된 토큰을 거부한다")
    void extractUserIdRejectsMalformedToken() {
        // given
        String validToken = refreshTokenService.issue(123);
        String truncatedToken = validToken.substring(0, validToken.length() / 2);

        // when & then
        assertInvalidRefreshToken(() -> refreshTokenService.extractUserId(truncatedToken));
    }

    @Test
    @DisplayName("extractUserId는 token_type 클레임이 누락된 토큰을 거부한다")
    void extractUserIdRejectsMissingTokenTypeClaim() throws JOSEException {
        // given
        String token = createTokenWithoutClaim(Integer.valueOf(11), VALID_ISSUER, Instant.now().plusSeconds(60),
            VALID_SECRET, "token_type");

        // when & then
        assertInvalidRefreshToken(() -> refreshTokenService.extractUserId(token));
    }

    @Test
    @DisplayName("extractUserId는 token_type 클레임이 비문자열 타입이면 거부한다")
    void extractUserIdRejectsNonStringTokenTypeClaim() throws JOSEException {
        // given
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(VALID_ISSUER)
            .issueTime(Date.from(Instant.now().minusSeconds(10)))
            .expirationTime(Date.from(Instant.now().plusSeconds(60)))
            .jwtID(UUID.randomUUID().toString())
            .claim("id", 11)
            .claim("token_type", 123) // 숫자 타입
            .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(VALID_SECRET));
        String token = jwt.serialize();

        // when & then
        assertInvalidRefreshToken(() -> refreshTokenService.extractUserId(token));
    }

    @Test
    @DisplayName("extractUserId는 id 클레임이 누락된 토큰을 거부한다")
    void extractUserIdRejectsMissingIdClaim() throws JOSEException {
        // given
        String token = createTokenWithoutClaim(Integer.valueOf(11), VALID_ISSUER, Instant.now().plusSeconds(60),
            VALID_SECRET, "id");

        // when & then
        assertInvalidRefreshToken(() -> refreshTokenService.extractUserId(token));
    }

    @Test
    @DisplayName("extractUserId는 id 클레임이 비숫자 타입이면 거부한다")
    void extractUserIdRejectsNonNumericIdClaim() throws JOSEException {
        // given
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(VALID_ISSUER)
            .issueTime(Date.from(Instant.now().minusSeconds(10)))
            .expirationTime(Date.from(Instant.now().plusSeconds(60)))
            .jwtID(UUID.randomUUID().toString())
            .claim("id", "not-a-number") // 문자열 타입
            .claim("token_type", "refresh")
            .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(VALID_SECRET));
        String token = jwt.serialize();

        // when & then
        assertInvalidRefreshToken(() -> refreshTokenService.extractUserId(token));
    }

    @Test
    @DisplayName("extractUserId는 issuer 클레임이 null이면 거부한다")
    void extractUserIdRejectsNullIssuerClaim() throws JOSEException {
        // given
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer((String)null) // null issuer
            .issueTime(Date.from(Instant.now().minusSeconds(10)))
            .expirationTime(Date.from(Instant.now().plusSeconds(60)))
            .jwtID(UUID.randomUUID().toString())
            .claim("id", 11)
            .claim("token_type", "refresh")
            .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(VALID_SECRET));
        String token = jwt.serialize();

        // when & then
        assertInvalidRefreshToken(() -> refreshTokenService.extractUserId(token));
    }

    @Test
    @DisplayName("extractUserId는 만료 시간이 현재 시간과 정확히 일치하는 경계를 테스트한다")
    void extractUserIdHandlesExpirationTimeBoundary() throws JOSEException, InterruptedException {
        // given - 현재 시간에서 1초 후에 만료되는 토큰 생성
        Instant expirationTime = Instant.now().plusSeconds(1);
        String token = createToken(11, VALID_ISSUER, "refresh", expirationTime, VALID_SECRET);

        // when - 즉시 검증하면 통과해야 함
        Integer userId = refreshTokenService.extractUserId(token);
        assertThat(userId).isEqualTo(11);

        // when - 2초 대기 후 만료된 토큰을 검증하면 실패해야 함
        Thread.sleep(2000);
        assertInvalidRefreshToken(() -> refreshTokenService.extractUserId(token));
    }

    @Test
    @DisplayName("extractUserId는 issuer가 빈 문자열인 토큰을 거부한다")
    void extractUserIdRejectsEmptyIssuerClaim() throws JOSEException {
        // given
        String token = createToken(11, "", "refresh", Instant.now().plusSeconds(60), VALID_SECRET);

        // when & then
        assertInvalidRefreshToken(() -> refreshTokenService.extractUserId(token));
    }

    @Test
    @DisplayName("rotate는 만료된 토큰으로 rotate 시도 시 INVALID_REFRESH_TOKEN 예외 발생")
    void rotateRejectsExpiredToken() throws JOSEException {
        // given
        String expiredToken = createToken(11, VALID_ISSUER, "refresh", Instant.now().minusSeconds(5), VALID_SECRET);

        // when & then
        assertInvalidRefreshToken(() -> refreshTokenService.rotate(expiredToken));
    }

    @Test
    @DisplayName("issue와 extractUserId는 토큰 claim 검증 왕복 테스트")
    void issueAndExtractUserIdClaimsRoundTrip() {
        // given
        Integer expectedUserId = 12345;

        // when
        String token = refreshTokenService.issue(expectedUserId);
        Integer extractedUserId = refreshTokenService.extractUserId(token);

        // then
        assertThat(extractedUserId).isEqualTo(expectedUserId);
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

    private String createTokenWithoutClaim(Integer userId, String issuer, Instant expiresAt, String secret,
        String claimToOmit)
        throws JOSEException {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .issueTime(Date.from(Instant.now().minusSeconds(10)))
            .expirationTime(Date.from(expiresAt))
            .jwtID(UUID.randomUUID().toString());

        if (!"id".equals(claimToOmit)) {
            builder.claim("id", userId);
        }
        if (!"token_type".equals(claimToOmit)) {
            builder.claim("token_type", "refresh");
        }

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
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
