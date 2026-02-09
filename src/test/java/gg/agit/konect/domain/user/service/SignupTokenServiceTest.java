package gg.agit.konect.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.global.auth.util.SecureTokenGenerator;
import gg.agit.konect.global.exception.CustomException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignupTokenService 단위 테스트")
class SignupTokenServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String TOKEN = "signup-token";
    private static final String PROVIDER_ID = "provider-id";
    private static final String INVALID_SIGNUP_TOKEN_MESSAGE = "회원가입 토큰이 올바르지 않습니다.";

    @InjectMocks
    private SignupTokenService signupTokenService;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private SecureTokenGenerator secureTokenGenerator;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Nested
    @DisplayName("issue 테스트")
    class IssueTests {

        @Test
        @DisplayName("signupTtl은 10분을 반환한다")
        void signupTtlReturnsTenMinutes() {
            // Given

            // When
            Duration ttl = signupTokenService.signupTtl();

            // Then
            assertThat(ttl).isEqualTo(Duration.ofMinutes(10));
        }

        @Test
        @DisplayName("email이 비어있으면 예외를 던진다")
        void issueBlankEmailThrowsException() {
            // Given

            // When & Then
            assertThatThrownBy(() -> signupTokenService.issue(" ", Provider.GOOGLE, PROVIDER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email and provider are required");
        }

        @Test
        @DisplayName("provider가 null이면 예외를 던진다")
        void issueNullProviderThrowsException() {
            // Given

            // When & Then
            assertThatThrownBy(() -> signupTokenService.issue(EMAIL, null, PROVIDER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email and provider are required");
        }

        @Test
        @DisplayName("유효한 입력이면 토큰을 발급하고 redis에 저장한다")
        void issueValidInputStoresSerializedClaims() {
            // Given
            given(secureTokenGenerator.generate()).willReturn(TOKEN);
            given(redis.opsForValue()).willReturn(valueOperations);

            // When
            String result = signupTokenService.issue(EMAIL, Provider.GOOGLE, PROVIDER_ID);

            // Then
            assertThat(result).isEqualTo(TOKEN);
            verify(valueOperations).set(
                eq("auth:signup:" + TOKEN),
                eq(EMAIL + "|GOOGLE|" + PROVIDER_ID),
                eq(Duration.ofMinutes(10))
            );
        }

        @Test
        @DisplayName("providerId가 null이면 빈 문자열로 직렬화 저장한다")
        void issueWithNullProviderIdStoresEmptyProviderId() {
            // Given
            given(secureTokenGenerator.generate()).willReturn(TOKEN);
            given(redis.opsForValue()).willReturn(valueOperations);

            // When
            signupTokenService.issue(EMAIL, Provider.APPLE, null);

            // Then
            verify(valueOperations).set(
                eq("auth:signup:" + TOKEN),
                eq(EMAIL + "|APPLE|"),
                eq(Duration.ofMinutes(10))
            );
        }
    }

    @Nested
    @DisplayName("consumeOrThrow 테스트")
    class ConsumeOrThrowTests {

        @Test
        @DisplayName("토큰이 비어있으면 예외를 던진다")
        void consumeOrThrowBlankTokenThrowsException() {
            // Given

            // When & Then
            assertThatThrownBy(() -> signupTokenService.consumeOrThrow(" "))
                .isInstanceOf(CustomException.class)
                .hasMessage(INVALID_SIGNUP_TOKEN_MESSAGE);
        }

        @Test
        @DisplayName("redis 값이 없으면 예외를 던진다")
        void consumeOrThrowWithoutRedisValueThrowsException() {
            // Given
            given(redis.execute(any(DefaultRedisScript.class), eq(List.of("auth:signup:" + TOKEN))))
                .willReturn(null);

            // When & Then
            assertThatThrownBy(() -> signupTokenService.consumeOrThrow(TOKEN))
                .isInstanceOf(CustomException.class)
                .hasMessage(INVALID_SIGNUP_TOKEN_MESSAGE);
        }

        @Test
        @DisplayName("잘못된 직렬화 값이면 예외를 던진다")
        void consumeOrThrowMalformedValueThrowsException() {
            // Given
            given(redis.execute(any(DefaultRedisScript.class), eq(List.of("auth:signup:" + TOKEN))))
                .willReturn("invalid-format");

            // When & Then
            assertThatThrownBy(() -> signupTokenService.consumeOrThrow(TOKEN))
                .isInstanceOf(CustomException.class)
                .hasMessage(INVALID_SIGNUP_TOKEN_MESSAGE);
        }

        @Test
        @DisplayName("provider enum 값이 잘못되면 예외를 던진다")
        void consumeOrThrowWithInvalidProviderThrowsException() {
            // Given
            given(redis.execute(any(DefaultRedisScript.class), eq(List.of("auth:signup:" + TOKEN))))
                .willReturn(EMAIL + "|INVALID|" + PROVIDER_ID);

            // When & Then
            assertThatThrownBy(() -> signupTokenService.consumeOrThrow(TOKEN))
                .isInstanceOf(CustomException.class)
                .hasMessage(INVALID_SIGNUP_TOKEN_MESSAGE);
        }

        @Test
        @DisplayName("유효한 직렬화 값이면 claims를 반환한다")
        void consumeOrThrowValidValueReturnsClaims() {
            // Given
            given(redis.execute(any(DefaultRedisScript.class), eq(List.of("auth:signup:" + TOKEN))))
                .willReturn(EMAIL + "|NAVER|" + PROVIDER_ID);

            // When
            SignupTokenService.SignupClaims claims = signupTokenService.consumeOrThrow(TOKEN);

            // Then
            assertThat(claims.email()).isEqualTo(EMAIL);
            assertThat(claims.provider()).isEqualTo(Provider.NAVER);
            assertThat(claims.providerId()).isEqualTo(PROVIDER_ID);
        }

        @Test
        @DisplayName("providerId가 빈 값이면 null로 반환한다")
        void consumeOrThrowWithoutProviderIdReturnsNullProviderId() {
            // Given
            given(redis.execute(any(DefaultRedisScript.class), eq(List.of("auth:signup:" + TOKEN))))
                .willReturn(EMAIL + "|APPLE|");

            // When
            SignupTokenService.SignupClaims claims = signupTokenService.consumeOrThrow(TOKEN);

            // Then
            assertThat(claims.email()).isEqualTo(EMAIL);
            assertThat(claims.provider()).isEqualTo(Provider.APPLE);
            assertThat(claims.providerId()).isNull();
        }
    }
}
