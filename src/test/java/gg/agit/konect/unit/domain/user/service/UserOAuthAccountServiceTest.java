package gg.agit.konect.unit.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
        User user = UserFixture.createUserWithId(1, "2021136001");
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
        User user = UserFixture.createUserWithId(1, "2021136001");
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
        User user = UserFixture.createUserWithId(1, "2021136001");
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
        User user = UserFixture.createUserWithId(1, "2021136001");
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
        User currentUser = UserFixture.createUserWithId(1, "2021136001");
        User withdrawnUser = UserFixture.createWithdrawnUser(2, "2020136002", LocalDateTime.now().minusDays(10));
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
        given(userOAuthAccountRepository.deleteRevokedExpiredWithdrawnOAuthAccountsBefore(
            now.minusDays(7),
            Provider.APPLE
        ))
            .willReturn(3);

        // when
        int deletedCount = userOAuthAccountService.cleanupExpiredWithdrawnUserOAuthAccounts(now);

        // then
        assertThat(deletedCount).isEqualTo(3);
        verify(userOAuthAccountRepository).deleteRevokedExpiredWithdrawnOAuthAccountsBefore(
            now.minusDays(7),
            Provider.APPLE
        );
        verify(userOAuthAccountRepository).flush();
    }

    @Test
    @DisplayName("getLinkStatus는 OAuth 계정이 없으면 빈 리스트를 반환한다")
    void getLinkStatusReturnsEmptyListWhenNoOAuthAccounts() {
        // given
        User user = UserFixture.createUserWithId(1, "2021136001");
        given(userRepository.getById(1)).willReturn(user);
        given(userOAuthAccountRepository.findAllByUserId(1)).willReturn(List.of());

        // when
        OAuthLinkStatusResponse response = userOAuthAccountService.getLinkStatus(1);

        // then
        assertThat(response.providers())
            .extracting(link -> link.provider(), link -> link.linked())
            .containsExactly(
                tuple(Provider.GOOGLE, false),
                tuple(Provider.NAVER, false),
                tuple(Provider.KAKAO, false),
                tuple(Provider.APPLE, false)
            );
    }

    @Test
    @DisplayName("linkPrimaryOAuthAccount는 providerId가 null이고 requireProviderId=false인 경우 성공한다")
    void linkPrimaryOAuthAccountAllowsNullProviderIdWhenNotRequired() {
        // given
        User user = UserFixture.createUserWithId(1, "2021136001");
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
            null,
            "google@konect.test",
            null
        );

        // then
        ArgumentCaptor<UserOAuthAccount> captor = ArgumentCaptor.forClass(UserOAuthAccount.class);
        verify(userOAuthAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getProvider()).isEqualTo(Provider.GOOGLE);
        assertThat(captor.getValue().getOauthEmail()).isEqualTo("google@konect.test");
        assertThat(captor.getValue().getProviderId()).isNull();
    }

    @Test
    @DisplayName("linkOAuthAccount는 providerId와 oauthEmail 모두 제공된 경우 성공한다")
    void linkOAuthAccountAcceptsBothProviderIdAndOauthEmail() {
        // given
        User user = UserFixture.createUserWithId(1, "2021136001");
        given(userRepository.getById(1)).willReturn(user);
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.GOOGLE, "google-provider-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.GOOGLE, "google-provider-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findAccountByProviderAndOauthEmail(Provider.GOOGLE, "google@konect.test"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByOauthEmailAndProvider("google@konect.test", Provider.GOOGLE))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findByUserIdAndProvider(1, Provider.GOOGLE))
            .willReturn(Optional.empty());

        // when
        userOAuthAccountService.linkOAuthAccount(
            1,
            Provider.GOOGLE,
            "google-provider-id",
            "google@konect.test",
            null
        );

        // then
        ArgumentCaptor<UserOAuthAccount> captor = ArgumentCaptor.forClass(UserOAuthAccount.class);
        verify(userOAuthAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getProviderId()).isEqualTo("google-provider-id");
        assertThat(captor.getValue().getOauthEmail()).isEqualTo("google@konect.test");
    }

    @Test
    @DisplayName("linkOAuthAccount는 providerId와 oauthEmail 모두 누락된 경우 예외를 던진다")
    void linkOAuthAccountRejectsWhenBothProviderIdAndOauthEmailMissing() {
        // given
        User user = UserFixture.createUserWithId(1, "2021136001");
        given(userRepository.getById(1)).willReturn(user);

        // when & then
        assertCustomException(
            ApiResponseCode.FAILED_EXTRACT_PROVIDER_ID,
            () -> userOAuthAccountService.linkOAuthAccount(
                1,
                Provider.GOOGLE,
                null,
                null,
                null
            )
        );
        verify(userOAuthAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("linkOAuthAccount는 oauthEmail이 빈 문자열인 경우에도 성공한다")
    void linkOAuthAccountAcceptsEmptyOauthEmail() {
        // given
        User user = UserFixture.createUserWithId(1, "2021136001");
        given(userRepository.getById(1)).willReturn(user);
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.GOOGLE, "google-provider-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.GOOGLE, "google-provider-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findByUserIdAndProvider(1, Provider.GOOGLE))
            .willReturn(Optional.empty());

        // when
        userOAuthAccountService.linkOAuthAccount(
            1,
            Provider.GOOGLE,
            "google-provider-id",
            "",
            null
        );

        // then
        ArgumentCaptor<UserOAuthAccount> captor = ArgumentCaptor.forClass(UserOAuthAccount.class);
        verify(userOAuthAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getProviderId()).isEqualTo("google-provider-id");
        assertThat(captor.getValue().getOauthEmail()).isEmpty();
    }

    @Test
    @DisplayName("restoreOrCleanupWithdrawnByLinkedProvider는 계정이 있지만 사용자가 탈퇴하지 않은 경우 아무것도 하지 않는다")
    void restoreOrCleanupWithdrawnByLinkedProviderDoesNothingWhenUserNotWithdrawn() {
        // given
        User activeUser = UserFixture.createUserWithId(1, "2021136001");
        UserOAuthAccount existingAccount = UserOAuthAccount.of(
            activeUser,
            Provider.GOOGLE,
            "google-provider-id",
            "google@konect.test",
            null
        );
        // activeUser는 deletedAt이 null이므로 restoreOrCleanupWithdrawnByLinkedProvider에서
        // isStageProfile()이 호출되지 않음 — environment stub 불필요
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.GOOGLE, "google-provider-id"))
            .willReturn(Optional.of(existingAccount));
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.GOOGLE, "google-provider-id"))
            .willReturn(Optional.of(activeUser));
        given(userOAuthAccountRepository.findAccountByProviderAndOauthEmail(Provider.GOOGLE, "new@konect.test"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByOauthEmailAndProvider("new@konect.test", Provider.GOOGLE))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findByUserIdAndProvider(1, Provider.GOOGLE))
            .willReturn(Optional.empty());

        // when
        userOAuthAccountService.linkPrimaryOAuthAccount(
            activeUser,
            Provider.GOOGLE,
            "google-provider-id",
            "new@konect.test",
            null
        );

        // then
        verify(userRepository, never()).save(any(User.class));
        verify(userOAuthAccountRepository, never()).delete(any(UserOAuthAccount.class));
    }

    @Test
    @DisplayName("restoreOrCleanupWithdrawnByLinkedProvider는 복구 기간 내인 경우 사용자를 복구한다")
    void restoreOrCleanupWithdrawnByLinkedProviderRestoresUserWithinWindow() {
        // given
        User currentUser = UserFixture.createUserWithId(1, "2021136001");
        User withdrawnUser = UserFixture.createWithdrawnUser(2, "2020136002", LocalDateTime.now().minusDays(3));
        UserOAuthAccount withdrawnAccount = UserOAuthAccount.of(
            withdrawnUser,
            Provider.GOOGLE,
            "google-provider-id",
            "old@konect.test",
            null
        );
        given(environment.acceptsProfiles(any(Profiles.class))).willReturn(false);
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.GOOGLE, "google-provider-id"))
            .willReturn(Optional.of(withdrawnAccount));
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.GOOGLE, "google-provider-id"))
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
            "google-provider-id",
            "new@konect.test",
            null
        );

        // then
        assertThat(withdrawnUser.getDeletedAt()).isNull();
        verify(userRepository).save(withdrawnUser);
        verify(userOAuthAccountRepository, never()).delete(any(UserOAuthAccount.class));
    }

    @Test
    @DisplayName("restoreOrCleanupWithdrawnByOauthEmail는 복구 기간 내인 경우 사용자를 복구한다")
    void restoreOrCleanupWithdrawnByOauthEmailRestoresUserWithinWindow() {
        // given
        User currentUser = UserFixture.createUserWithId(1, "2021136001");
        User withdrawnUser = UserFixture.createWithdrawnUser(2, "2020136002", LocalDateTime.now().minusDays(3));
        UserOAuthAccount withdrawnAccount = UserOAuthAccount.of(
            withdrawnUser,
            Provider.GOOGLE,
            "google-provider-id",
            "old@konect.test",
            null
        );
        given(environment.acceptsProfiles(any(Profiles.class))).willReturn(false);
        given(userOAuthAccountRepository.findAccountByProviderAndOauthEmail(Provider.GOOGLE, "old@konect.test"))
            .willReturn(Optional.of(withdrawnAccount));
        given(userOAuthAccountRepository.findUserByOauthEmailAndProvider("old@konect.test", Provider.GOOGLE))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.GOOGLE, "google-provider-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.GOOGLE, "google-provider-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findByUserIdAndProvider(1, Provider.GOOGLE))
            .willReturn(Optional.empty());

        // when
        userOAuthAccountService.linkPrimaryOAuthAccount(
            currentUser,
            Provider.GOOGLE,
            "google-provider-id",
            "old@konect.test",
            null
        );

        // then
        assertThat(withdrawnUser.getDeletedAt()).isNull();
        verify(userRepository).save(withdrawnUser);
        verify(userOAuthAccountRepository, never()).delete(any(UserOAuthAccount.class));
    }

    @Test
    @DisplayName("getPrimaryOAuthAccount는 계정이 없는 경우 null을 반환한다")
    void getPrimaryOAuthAccountReturnsNullWhenNoAccounts() {
        // given
        given(userOAuthAccountRepository.findAllByUserId(1)).willReturn(List.of());

        // when
        UserOAuthAccount result = userOAuthAccountService.getPrimaryOAuthAccount(1);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Stage profile에서 탈퇴 계정은 복구하지 않고 삭제된다")
    void stageProfileDeletesWithdrawnAccountWithoutRestore() {
        // given
        User currentUser = UserFixture.createUserWithId(1, "2021136001");
        User withdrawnUser = UserFixture.createWithdrawnUser(2, "2020136002", LocalDateTime.now().minusDays(3));
        UserOAuthAccount withdrawnAccount = UserOAuthAccount.of(
            withdrawnUser,
            Provider.GOOGLE,
            "google-provider-id",
            "old@konect.test",
            null
        );
        given(environment.acceptsProfiles(any(Profiles.class))).willReturn(true);
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.GOOGLE, "google-provider-id"))
            .willReturn(Optional.of(withdrawnAccount));
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.GOOGLE, "google-provider-id"))
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
            "google-provider-id",
            "new@konect.test",
            null
        );

        // then
        verify(userOAuthAccountRepository).delete(withdrawnAccount);
        verify(userOAuthAccountRepository).flush();
        verify(userRepository, never()).save(withdrawnUser);
    }

    @Test
    @DisplayName("providerId가 NULL인 기존 계정에 새 providerId 연동 시 다른 사용자가 사용 중이면 충돌 예외 발생")
    void linkOAuthAccountRejectsWhenNewProviderIdAlreadyLinkedToDifferentUser() {
        // given
        User currentUser = UserFixture.createUserWithId(1, "2021136001");
        User otherUser = UserFixture.createUserWithId(2, "2022136002");
        given(userRepository.getById(1)).willReturn(currentUser);
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.GOOGLE, "new-provider-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.GOOGLE, "new-provider-id"))
            .willReturn(Optional.of(otherUser));

        // when & then
        assertCustomException(
            ApiResponseCode.OAUTH_ACCOUNT_ALREADY_LINKED,
            () -> userOAuthAccountService.linkOAuthAccount(
                1,
                Provider.GOOGLE,
                "new-provider-id",
                "current@konect.test",
                null
            )
        );
        verify(userOAuthAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("복구 기간 정확히 7일 경계값에서는 복구 불가능")
    void restoreWindowBoundaryAtExactlySevenDaysCannotRestore() {
        // given
        User currentUser = UserFixture.createUserWithId(1, "2021136001");
        User withdrawnUser = UserFixture.createWithdrawnUser(2, "2020136002", LocalDateTime.now().minusDays(7));
        UserOAuthAccount withdrawnAccount = UserOAuthAccount.of(
            withdrawnUser,
            Provider.GOOGLE,
            "google-provider-id",
            "old@konect.test",
            null
        );
        given(environment.acceptsProfiles(any(Profiles.class))).willReturn(false);
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.GOOGLE, "google-provider-id"))
            .willReturn(Optional.of(withdrawnAccount));
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.GOOGLE, "google-provider-id"))
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
            "google-provider-id",
            "new@konect.test",
            null
        );

        // then - 정확히 7일 경과 시 isAfter()가 false를 반환하므로 삭제됨
        verify(userOAuthAccountRepository).delete(withdrawnAccount);
        verify(userOAuthAccountRepository).flush();
        verify(userRepository, never()).save(withdrawnUser);
    }

    @Test
    @DisplayName("providerId와 oauthEmail이 서로 다른 탈퇴 사용자인 경우 모두 정리된다")
    void linkOAuthAccountCleansUpBothWithdrawnUsersFromProviderIdAndOauthEmail() {
        // given
        User currentUser = UserFixture.createUserWithId(1, "2021136001");
        User withdrawnUserByProviderId = UserFixture.createWithdrawnUser(2, "2020136002",
            LocalDateTime.now().minusDays(10));
        User withdrawnUserByOauthEmail = UserFixture.createWithdrawnUser(3, "2020136003",
            LocalDateTime.now().minusDays(10));

        UserOAuthAccount accountByProviderId = UserOAuthAccount.of(
            withdrawnUserByProviderId,
            Provider.GOOGLE,
            "google-provider-id",
            "old1@konect.test",
            null
        );

        UserOAuthAccount accountByOauthEmail = UserOAuthAccount.of(
            withdrawnUserByOauthEmail,
            Provider.GOOGLE,
            "other-provider-id",
            "old2@konect.test",
            null
        );

        given(environment.acceptsProfiles(any(Profiles.class))).willReturn(false);
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.GOOGLE, "google-provider-id"))
            .willReturn(Optional.of(accountByProviderId));
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.GOOGLE, "google-provider-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findAccountByProviderAndOauthEmail(Provider.GOOGLE, "old2@konect.test"))
            .willReturn(Optional.of(accountByOauthEmail));
        given(userOAuthAccountRepository.findUserByOauthEmailAndProvider("old2@konect.test", Provider.GOOGLE))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findByUserIdAndProvider(1, Provider.GOOGLE))
            .willReturn(Optional.empty());

        // when
        userOAuthAccountService.linkPrimaryOAuthAccount(
            currentUser,
            Provider.GOOGLE,
            "google-provider-id",
            "old2@konect.test",
            null
        );

        // then
        verify(userOAuthAccountRepository).delete(accountByProviderId);
        verify(userOAuthAccountRepository).delete(accountByOauthEmail);
        verify(userOAuthAccountRepository, times(2)).flush();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("기존 계정의 providerId 업데이트 시 다른 providerId로 충돌하면 예외 발생")
    void linkOAuthAccountRejectsProviderIdUpdateWhenConflict() {
        // given
        User user = UserFixture.createUserWithId(1, "2021136001");
        UserOAuthAccount existingAccount = UserOAuthAccount.of(
            user,
            Provider.GOOGLE,
            "old-provider-id",
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
        verify(userOAuthAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Apple provider는 appleRefreshToken 업데이트를 호출한다")
    void linkOAuthAccountUpdatesAppleRefreshTokenForAppleProvider() {
        // given
        User user = UserFixture.createUserWithId(1, "2021136001");
        UserOAuthAccount existingAccount = UserOAuthAccount.of(
            user,
            Provider.APPLE,
            "apple-provider-id",
            "apple@konect.test",
            "old-refresh-token"
        );
        given(userRepository.getById(1)).willReturn(user);
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.APPLE, "apple-provider-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.APPLE, "apple-provider-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findAccountByProviderAndOauthEmail(Provider.APPLE, "apple@konect.test"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByOauthEmailAndProvider("apple@konect.test", Provider.APPLE))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findByUserIdAndProvider(1, Provider.APPLE))
            .willReturn(Optional.of(existingAccount));

        // when
        userOAuthAccountService.linkOAuthAccount(
            1,
            Provider.APPLE,
            "apple-provider-id",
            "apple@konect.test",
            "new-refresh-token"
        );

        // then
        assertThat(existingAccount.getAppleRefreshToken()).isEqualTo("new-refresh-token");
        verify(userOAuthAccountRepository).save(existingAccount);
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
