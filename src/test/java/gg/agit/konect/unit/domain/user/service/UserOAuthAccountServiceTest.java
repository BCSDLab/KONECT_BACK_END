package gg.agit.konect.unit.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.dto.OAuthLinkStatusResponse;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.model.UserOAuthAccount;
import gg.agit.konect.domain.user.repository.UserOAuthAccountRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.domain.user.service.UserOAuthAccountService;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.UserFixture;

class UserOAuthAccountServiceTest extends ServiceTestSupport {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserOAuthAccountRepository userOAuthAccountRepository;

    @Mock
    private Environment environment;

    @InjectMocks
    private UserOAuthAccountService userOAuthAccountService;

    @Test
    @DisplayName("getLinkStatus는 모든 provider에 대한 연동 여부를 반환한다")
    void getLinkStatusReturnsEveryProvider() {
        // given
        User user = UserFixture.createUserWithId(1, "2021136001");
        given(userRepository.getById(1)).willReturn(user);
        given(userOAuthAccountRepository.findAllByUserId(1)).willReturn(List.of(
            UserOAuthAccount.of(user, Provider.GOOGLE, "google-id", "google@konect.test", null),
            UserOAuthAccount.of(user, Provider.APPLE, "apple-id", "apple@konect.test", "apple-refresh")
        ));

        // when
        OAuthLinkStatusResponse response = userOAuthAccountService.getLinkStatus(1);

        // then
        assertThat(response.providers())
            .extracting(link -> link.provider(), link -> link.linked())
            .containsExactly(
                tuple(Provider.GOOGLE, true),
                tuple(Provider.NAVER, false),
                tuple(Provider.KAKAO, false),
                tuple(Provider.APPLE, true)
            );
    }

    @Test
    @DisplayName("linkOAuthAccount는 providerId가 비어 있으면 예외를 던진다")
    void linkOAuthAccountRejectsBlankProviderId() {
        // given
        User user = createUser(1, "2021136001");
        given(userRepository.getById(1)).willReturn(user);

        // when & then
        assertCustomException(
            ApiResponseCode.FAILED_EXTRACT_PROVIDER_ID,
            () -> userOAuthAccountService.linkOAuthAccount(
                1,
                Provider.GOOGLE,
                " ",
                "google@konect.test",
                null
            )
        );
        verify(userOAuthAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("linkPrimaryOAuthAccount는 providerId가 없어도 이메일 기준으로 새 계정을 생성한다")
    void linkPrimaryOAuthAccountAllowsBlankProviderId() {
        // given
        User user = createUser(1, "2021136001");
        given(userOAuthAccountRepository.findAccountByProviderAndOauthEmail(Provider.GOOGLE, "google@konect.test"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByOauthEmailAndProvider("google@konect.test", Provider.GOOGLE))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findByUserIdAndProvider(1, Provider.GOOGLE))
            .willReturn(Optional.empty());

        // when
        userOAuthAccountService.linkPrimaryOAuthAccount(
            user,
            Provider.GOOGLE,
            " ",
            "google@konect.test",
            null
        );

        // then
        ArgumentCaptor<UserOAuthAccount> captor = ArgumentCaptor.forClass(UserOAuthAccount.class);
        verify(userOAuthAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getProvider()).isEqualTo(Provider.GOOGLE);
        assertThat(captor.getValue().getOauthEmail()).isEqualTo("google@konect.test");
    }

    @Test
    @DisplayName("linkOAuthAccount는 비어 있던 providerId와 Apple refresh token을 기존 계정에 채운다")
    void linkOAuthAccountUpdatesExistingAccountWhenProviderIdWasMissing() {
        // given
        User user = createUser(1, "2021136001");
        UserOAuthAccount existingAccount = UserOAuthAccount.of(
            user,
            Provider.APPLE,
            null,
            "old@konect.test",
            null
        );
        given(userRepository.getById(1)).willReturn(user);
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.APPLE, "apple-provider-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.APPLE, "apple-provider-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findAccountByProviderAndOauthEmail(Provider.APPLE, "new@konect.test"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByOauthEmailAndProvider("new@konect.test", Provider.APPLE))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findByUserIdAndProvider(1, Provider.APPLE))
            .willReturn(Optional.of(existingAccount));

        // when
        userOAuthAccountService.linkOAuthAccount(
            1,
            Provider.APPLE,
            "apple-provider-id",
            "new@konect.test",
            "apple-refresh-token"
        );

        // then
        assertThat(existingAccount.getProviderId()).isEqualTo("apple-provider-id");
        assertThat(existingAccount.getOauthEmail()).isEqualTo("new@konect.test");
        assertThat(existingAccount.getAppleRefreshToken()).isEqualTo("apple-refresh-token");
        verify(userOAuthAccountRepository).save(existingAccount);
    }

    @Test
    @DisplayName("linkOAuthAccount는 이미 다른 providerId가 있으면 충돌 예외를 던진다")
    void linkOAuthAccountRejectsConflictingProviderIdOnExistingAccount() {
        // given
        User user = createUser(1, "2021136001");
        UserOAuthAccount existingAccount = UserOAuthAccount.of(
            user,
            Provider.GOOGLE,
            "existing-provider-id",
            "google@konect.test",
            null
        );
        given(userRepository.getById(1)).willReturn(user);
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.GOOGLE, "new-provider-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.GOOGLE, "new-provider-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findAccountByProviderAndOauthEmail(Provider.GOOGLE, "google@konect.test"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByOauthEmailAndProvider("google@konect.test", Provider.GOOGLE))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findByUserIdAndProvider(1, Provider.GOOGLE))
            .willReturn(Optional.of(existingAccount));

        // when & then
        assertCustomException(
            ApiResponseCode.OAUTH_PROVIDER_ALREADY_LINKED,
            () -> userOAuthAccountService.linkOAuthAccount(
                1,
                Provider.GOOGLE,
                "new-provider-id",
                "google@konect.test",
                null
            )
        );
        verify(userOAuthAccountRepository, never()).save(existingAccount);
    }

    @Test
    @DisplayName("linkPrimaryOAuthAccount는 복구 기간이 지난 탈퇴 계정을 정리하고 새 계정을 저장한다")
    void linkPrimaryOAuthAccountDeletesExpiredWithdrawnAccountBeforeSavingReplacement() {
        // given
        User currentUser = createUser(1, "2021136001");
        User withdrawnUser = createWithdrawnUser(2, "2020136002", LocalDateTime.now().minusDays(10));
        UserOAuthAccount withdrawnAccount = UserOAuthAccount.of(
            withdrawnUser,
            Provider.GOOGLE,
            "expired-provider-id",
            "old@konect.test",
            null
        );
        given(environment.acceptsProfiles(any(Profiles.class))).willReturn(false);
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.GOOGLE, "expired-provider-id"))
            .willReturn(Optional.of(withdrawnAccount));
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.GOOGLE, "expired-provider-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findAccountByProviderAndOauthEmail(Provider.GOOGLE, "new@konect.test"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByOauthEmailAndProvider("new@konect.test", Provider.GOOGLE))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findByUserIdAndProvider(1, Provider.GOOGLE))
            .willReturn(Optional.empty());

        // when
        userOAuthAccountService.linkPrimaryOAuthAccount(
            currentUser,
            Provider.GOOGLE,
            "expired-provider-id",
            "new@konect.test",
            null
        );

        // then
        verify(userOAuthAccountRepository).delete(withdrawnAccount);
        verify(userOAuthAccountRepository).flush();
        verify(userRepository, never()).save(withdrawnUser);
        verify(userOAuthAccountRepository).save(any(UserOAuthAccount.class));
    }

    @Test
    @DisplayName("cleanupExpiredWithdrawnUserOAuthAccounts는 임계 시각으로 삭제 후 flush한다")
    void cleanupExpiredWithdrawnUserOAuthAccountsDeletesUsingThreshold() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 4, 10, 9, 30);
        given(userOAuthAccountRepository.deleteAllByWithdrawnUsersBefore(now.minusDays(7))).willReturn(3);

        // when
        int deletedCount = userOAuthAccountService.cleanupExpiredWithdrawnUserOAuthAccounts(now);

        // then
        assertThat(deletedCount).isEqualTo(3);
        verify(userOAuthAccountRepository).deleteAllByWithdrawnUsersBefore(now.minusDays(7));
        verify(userOAuthAccountRepository).flush();
    }

    private User createWithdrawnUser(Integer id, String studentNumber, LocalDateTime deletedAt) {
        User user = UserFixture.createUserWithId(id, studentNumber);
        user.withdraw(deletedAt);
        return user;
    }

    private void assertCustomException(ApiResponseCode expectedErrorCode, ThrowingCallable callable) {
        assertThatThrownBy(callable::call)
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode()).isEqualTo(
                expectedErrorCode
            ));
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }
}
