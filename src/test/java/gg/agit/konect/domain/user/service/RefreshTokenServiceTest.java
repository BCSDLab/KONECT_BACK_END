package gg.agit.konect.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import gg.agit.konect.global.auth.util.SecureTokenGenerator;
import gg.agit.konect.global.exception.CustomException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService 단위 테스트")
class RefreshTokenServiceTest {

    private static final int USER_ID = 10;
    private static final String OLD_TOKEN = "old-token";
    private static final String NEW_TOKEN = "new-token";
    private static final String INVALID_REFRESH_TOKEN_MESSAGE = "리프레시 토큰이 올바르지 않습니다.";

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private SecureTokenGenerator secureTokenGenerator;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Nested
    @DisplayName("issue 테스트")
    class IssueTests {

        @Test
        @DisplayName("refreshTtl은 30일을 반환한다")
        void refreshTtlReturnsThirtyDays() {
            // Given

            // When
            Duration ttl = refreshTokenService.refreshTtl();

            // Then
            assertThat(ttl).isEqualTo(Duration.ofDays(30));
        }

        @Test
        @DisplayName("userId가 null이면 예외를 던진다")
        void issueNullUserIdThrowsException() {
            // Given

            // When & Then
            assertThatThrownBy(() -> refreshTokenService.issue(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId is required");
        }

        @Test
        @DisplayName("유효한 userId면 토큰을 생성하고 redis에 저장한다")
        void issueValidUserIdSavesTokenInRedis() {
            // Given
            given(redis.opsForValue()).willReturn(valueOperations);
            given(redis.opsForSet()).willReturn(setOperations);
            given(secureTokenGenerator.generate()).willReturn(NEW_TOKEN);

            // When
            String result = refreshTokenService.issue(USER_ID);

            // Then
            assertThat(result).isEqualTo(NEW_TOKEN);
            verify(valueOperations).set(
                eq("auth:refresh:active:" + NEW_TOKEN),
                eq(String.valueOf(USER_ID)),
                eq(Duration.ofDays(30))
            );
            verify(setOperations).add("auth:refresh:user:" + USER_ID, NEW_TOKEN);
            verify(redis).expire("auth:refresh:user:" + USER_ID, Duration.ofDays(30));
        }
    }

    @Nested
    @DisplayName("rotate 테스트")
    class RotateTests {

        @Test
        @DisplayName("refreshToken이 비어있으면 예외를 던진다")
        void rotateEmptyTokenThrowsException() {
            // Given

            // When & Then
            assertThatThrownBy(() -> refreshTokenService.rotate(" "))
                .isInstanceOf(CustomException.class)
                .hasMessage(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        @Test
        @DisplayName("토큰이 유효하지 않으면 예외를 던진다")
        void rotateInvalidTokenThrowsException() {
            // Given
            given(redis.execute(any(DefaultRedisScript.class), eq(List.of("auth:refresh:active:" + OLD_TOKEN))))
                .willReturn(null);

            // When & Then
            assertThatThrownBy(() -> refreshTokenService.rotate(OLD_TOKEN))
                .isInstanceOf(CustomException.class)
                .hasMessage(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        @Test
        @DisplayName("토큰 값이 숫자가 아니면 예외를 던진다")
        void rotateWithNonNumericUserIdThrowsException() {
            // Given
            given(redis.execute(any(DefaultRedisScript.class), eq(List.of("auth:refresh:active:" + OLD_TOKEN))))
                .willReturn("not-a-number");

            // When & Then
            assertThatThrownBy(() -> refreshTokenService.rotate(OLD_TOKEN))
                .isInstanceOf(CustomException.class)
                .hasMessage(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        @Test
        @DisplayName("토큰이 유효하면 토큰을 회전시키고 새 토큰을 반환한다")
        void rotateValidTokenReturnsNewToken() {
            // Given
            given(redis.execute(any(DefaultRedisScript.class), eq(List.of("auth:refresh:active:" + OLD_TOKEN))))
                .willReturn(String.valueOf(USER_ID));
            given(redis.opsForSet()).willReturn(setOperations);
            given(redis.opsForValue()).willReturn(valueOperations);
            given(secureTokenGenerator.generate()).willReturn(NEW_TOKEN);

            // When
            RefreshTokenService.Rotated rotated = refreshTokenService.rotate(OLD_TOKEN);

            // Then
            assertThat(rotated.userId()).isEqualTo(USER_ID);
            assertThat(rotated.refreshToken()).isEqualTo(NEW_TOKEN);
            verify(setOperations).remove("auth:refresh:user:" + USER_ID, OLD_TOKEN);
            verify(valueOperations).set(
                eq("auth:refresh:active:" + NEW_TOKEN),
                eq(String.valueOf(USER_ID)),
                eq(Duration.ofDays(30))
            );
        }
    }

    @Nested
    @DisplayName("revoke 테스트")
    class RevokeTests {

        @Test
        @DisplayName("refreshToken이 비어있으면 아무 동작도 하지 않는다")
        void revokeBlankTokenDoesNothing() {
            // Given

            // When
            refreshTokenService.revoke(" ");

            // Then
            verify(redis, never()).execute(any(DefaultRedisScript.class), anyList());
        }

        @Test
        @DisplayName("유효한 토큰이면 active key를 지우고 user set에서 제거한다")
        void revokeValidTokenRemovesFromRedisSet() {
            // Given
            given(redis.execute(any(DefaultRedisScript.class), eq(List.of("auth:refresh:active:" + OLD_TOKEN))))
                .willReturn(String.valueOf(USER_ID));
            given(redis.opsForSet()).willReturn(setOperations);

            // When
            refreshTokenService.revoke(OLD_TOKEN);

            // Then
            verify(setOperations).remove("auth:refresh:user:" + USER_ID, OLD_TOKEN);
        }

        @Test
        @DisplayName("토큰 값이 숫자가 아니면 user set에서 제거하지 않는다")
        void revokeWithNonNumericUserIdDoesNotRemoveFromSet() {
            // Given
            given(redis.execute(any(DefaultRedisScript.class), eq(List.of("auth:refresh:active:" + OLD_TOKEN))))
                .willReturn("invalid-user-id");

            // When
            refreshTokenService.revoke(OLD_TOKEN);

            // Then
            verify(setOperations, never()).remove(any(String.class), any(String.class));
        }
    }

    @Nested
    @DisplayName("revokeAll 테스트")
    class RevokeAllTests {

        @Test
        @DisplayName("userId가 null이면 아무 동작도 하지 않는다")
        void revokeAllNullUserIdDoesNothing() {
            // Given

            // When
            refreshTokenService.revokeAll(null);

            // Then
            verify(redis, never()).opsForSet();
        }

        @Test
        @DisplayName("토큰이 없으면 user set key만 삭제한다")
        void revokeAllWithoutTokensDeletesSetKey() {
            // Given
            given(redis.opsForSet()).willReturn(setOperations);
            given(setOperations.members("auth:refresh:user:" + USER_ID)).willReturn(Set.of());

            // When
            refreshTokenService.revokeAll(USER_ID);

            // Then
            verify(redis).delete("auth:refresh:user:" + USER_ID);
        }

        @Test
        @DisplayName("members 결과가 null이어도 user set key를 삭제한다")
        void revokeAllWithNullMembersDeletesSetKey() {
            // Given
            given(redis.opsForSet()).willReturn(setOperations);
            given(setOperations.members("auth:refresh:user:" + USER_ID)).willReturn(null);

            // When
            refreshTokenService.revokeAll(USER_ID);

            // Then
            verify(redis).delete("auth:refresh:user:" + USER_ID);
        }

        @Test
        @DisplayName("토큰이 있으면 active key들과 user set key를 모두 삭제한다")
        void revokeAllWithTokensDeletesAllKeys() {
            // Given
            given(redis.opsForSet()).willReturn(setOperations);
            given(setOperations.members("auth:refresh:user:" + USER_ID)).willReturn(Set.of("a", "b"));

            // When
            refreshTokenService.revokeAll(USER_ID);

            // Then
            verify(redis).delete("auth:refresh:active:a");
            verify(redis).delete("auth:refresh:active:b");
            verify(redis).delete("auth:refresh:user:" + USER_ID);
        }
    }
}
