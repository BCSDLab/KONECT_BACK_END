package gg.agit.konect.unit.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.service.SignupTokenService;
import gg.agit.konect.global.auth.util.SecureTokenGenerator;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;

class SignupTokenServiceTest extends ServiceTestSupport {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SecureTokenGenerator secureTokenGenerator;

    @InjectMocks
    private SignupTokenService signupTokenService;

    @Test
    @DisplayName("issue는 토큰을 생성하고 claims를 TTL과 함께 저장한다")
    void issueStoresSerializedClaimsWithTtl() {
        // given
        given(redis.opsForValue()).willReturn(valueOperations);
        given(secureTokenGenerator.generate()).willReturn("signup-token");

        // when
        String token = signupTokenService.issue(
            "user@koreatech.ac.kr",
            Provider.GOOGLE,
            "provider-123",
            "홍길동"
        );

        // then
        assertThat(token).isEqualTo("signup-token");
        verify(valueOperations).set(
            eq("auth:signup:signup-token"),
            eq("user@koreatech.ac.kr|GOOGLE|provider-123|홍길동"),
            eq(Duration.ofMinutes(10))
        );
    }

    @Test
    @DisplayName("readOrThrow는 providerId와 name이 비어 있으면 null로 복원한다")
    void readOrThrowNormalizesBlankProviderIdAndName() {
        // given
        given(redis.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("auth:signup:signup-token"))
            .willReturn("user@koreatech.ac.kr|APPLE||");

        // when
        SignupTokenService.SignupClaims claims = signupTokenService.readOrThrow("signup-token");

        // then
        assertThat(claims.email()).isEqualTo("user@koreatech.ac.kr");
        assertThat(claims.provider()).isEqualTo(Provider.APPLE);
        assertThat(claims.providerId()).isNull();
        assertThat(claims.name()).isNull();
    }

    @Test
    @DisplayName("readOrThrow는 잘못 직렬화된 토큰이면 INVALID_SIGNUP_TOKEN을 던진다")
    void readOrThrowThrowsWhenSerializedClaimsAreInvalid() {
        // given
        given(redis.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("auth:signup:broken-token")).willReturn("broken|GOOGLE");

        // when & then
        assertInvalidSignupToken(() -> signupTokenService.readOrThrow("broken-token"));
    }

    @Test
    @DisplayName("consumeOrThrow는 토큰을 한 번만 읽고 삭제한다")
    void consumeOrThrowReadsAndDeletesTokenAtomically() {
        // given
        given(redis.execute(any(DefaultRedisScript.class), eq(List.of("auth:signup:signup-token"))))
            .willReturn("user@koreatech.ac.kr|KAKAO|provider-1|코넥트");

        // when
        SignupTokenService.SignupClaims claims = signupTokenService.consumeOrThrow("signup-token");

        // then
        assertThat(claims.email()).isEqualTo("user@koreatech.ac.kr");
        assertThat(claims.provider()).isEqualTo(Provider.KAKAO);
        assertThat(claims.providerId()).isEqualTo("provider-1");
        assertThat(claims.name()).isEqualTo("코넥트");
        verify(redis, never()).opsForValue();
    }

    @Test
    @DisplayName("issue는 email 또는 provider가 비어 있으면 IllegalArgumentException을 던진다")
    void issueRejectsMissingRequiredFields() {
        assertThatThrownBy(() -> signupTokenService.issue(" ", Provider.GOOGLE, "provider-1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("email and provider are required");

        assertThatThrownBy(() -> signupTokenService.issue("user@koreatech.ac.kr", null, "provider-1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("email and provider are required");
    }

    @Test
    @DisplayName("consumeOrThrow는 빈 토큰이면 Redis를 조회하지 않고 INVALID_SIGNUP_TOKEN을 던진다")
    void consumeOrThrowRejectsBlankTokenWithoutRedisLookup() {
        // when & then
        assertInvalidSignupToken(() -> signupTokenService.consumeOrThrow(" "));
        verify(redis, never()).execute(any(DefaultRedisScript.class), any());
    }

    @Test
    @DisplayName("issue는 빈 이메일 문자열을 거부한다")
    void issueRejectsEmptyEmail() {
        assertThatThrownBy(() -> signupTokenService.issue("", Provider.GOOGLE, "provider-1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("email and provider are required");
    }

    @Test
    @DisplayName("deserialize는 빈 파트를 포함한 직렬화된 데이터를 거부한다")
    void deserializeRejectsEmptyParts() {
        // "email|||" → split 결과: [email, "", "", ""] → provider가 빈 문자열이므로 거부
        given(redis.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("auth:signup:token")).willReturn("email|||");

        assertInvalidSignupToken(() -> signupTokenService.readOrThrow("token"));
    }

    @Test
    @DisplayName("deserialize는 4개 초과 파트를 거부한다")
    void deserializeRejectsMoreThanFourParts() {
        // given
        given(redis.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("auth:signup:token")).willReturn("a|b|c|d|e");

        // when & then
        assertInvalidSignupToken(() -> signupTokenService.readOrThrow("token"));
    }

    @Test
    @DisplayName("deserialize는 빈 이메일 필드를 거부한다")
    void deserializeRejectsEmptyEmailField() {
        // given
        given(redis.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("auth:signup:token")).willReturn("|GOOGLE|provider-1|name");

        // when & then
        assertInvalidSignupToken(() -> signupTokenService.readOrThrow("token"));
    }

    @Test
    @DisplayName("deserialize는 빈 프로바이더 필드를 거부한다")
    void deserializeRejectsEmptyProviderField() {
        // given
        given(redis.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("auth:signup:token")).willReturn("email||provider-1|name");

        // when & then
        assertInvalidSignupToken(() -> signupTokenService.readOrThrow("token"));
    }

    @Test
    @DisplayName("deserialize는 유효하지 않은 Provider enum 값을 거부한다")
    void deserializeRejectsInvalidProviderValue() {
        // given
        given(redis.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("auth:signup:token")).willReturn("email|INVALID_PROVIDER|provider-1|name");

        // when & then
        assertInvalidSignupToken(() -> signupTokenService.readOrThrow("token"));
    }

    @Test
    @DisplayName("readOrThrow는 Redis가 빈 문자열을 반환하면 INVALID_SIGNUP_TOKEN을 던진다")
    void readOrThrowRejectsEmptyStringFromRedis() {
        // given
        given(redis.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("auth:signup:token")).willReturn("");

        // when & then
        assertInvalidSignupToken(() -> signupTokenService.readOrThrow("token"));
    }

    @Test
    @DisplayName("consumeOrThrow는 Redis가 빈 문자열을 반환하면 INVALID_SIGNUP_TOKEN을 던진다")
    void consumeOrThrowRejectsEmptyStringFromRedis() {
        // given
        given(redis.execute(any(DefaultRedisScript.class), eq(List.of("auth:signup:token"))))
            .willReturn("");

        // when & then
        assertInvalidSignupToken(() -> signupTokenService.consumeOrThrow("token"));
    }

    private void assertInvalidSignupToken(ThrowingCallable callable) {
        assertThatThrownBy(callable::call)
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode())
                .isEqualTo(ApiResponseCode.INVALID_SIGNUP_TOKEN));
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }
}
