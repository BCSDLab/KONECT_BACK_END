package gg.agit.konect.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;

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
    @DisplayName("getLinkStatus는 모든 Provider를 반환하고 연동 여부를 반영한다")
    void getLinkStatusReturnsAllProvidersWithLinkedFlags() {
        // given
        User user = createUser(1, "2021136001");
        given(userRepository.getById(1)).willReturn(user);
        given(userOAuthAccountRepository.findAllByUserId(1)).willReturn(List.of(
            UserOAuthAccount.of(user, Provider.GOOGLE, "google-id", "user@google.com", null),
            UserOAuthAccount.of(user, Provider.APPLE, "apple-id", "user@apple.com", "refresh-token")
        ));

        // when
        OAuthLinkStatusResponse response = userOAuthAccountService.getLinkStatus(1);

        // then
        assertThat(response.providers())
            .extracting(status -> status.provider().name() + ":" + status.linked())
            .containsExactly(
                "GOOGLE:true",
                "NAVER:false",
                "KAKAO:false",
                "APPLE:true"
            );
    }

    @Test
    @DisplayName("linkOAuthAccount는 providerId가 비어 있으면 예외를 던진다")
    void linkOAuthAccountRejectsBlankProviderId() {
        // given
        User user = createUser(1, "2021136001");
        given(userRepository.getById(1)).willReturn(user);

        // when & then
        assertCustomExceptionCode(
            () -> userOAuthAccountService.linkOAuthAccount(1, Provider.GOOGLE, " ", "user@google.com", null),
            ApiResponseCode.FAILED_EXTRACT_PROVIDER_ID
        );
        verify(userOAuthAccountRepository, never()).save(any(UserOAuthAccount.class));
    }

    @Test
    @DisplayName("linkPrimaryOAuthAccount는 providerId가 없어도 이메일 기반으로 새 계정을 저장한다")
    void linkPrimaryOAuthAccountAllowsMissingProviderId() {
        // given
        User user = createUser(1, "2021136001");
        given(userOAuthAccountRepository.findAccountByProviderAndOauthEmail(Provider.APPLE, "user@apple.com"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByOauthEmailAndProvider("user@apple.com", Provider.APPLE))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findByUserIdAndProvider(1, Provider.APPLE)).willReturn(Optional.empty());

        // when
        userOAuthAccountService.linkPrimaryOAuthAccount(user, Provider.APPLE, null, "user@apple.com", "refresh-token");

        // then
        ArgumentCaptor<UserOAuthAccount> accountCaptor = ArgumentCaptor.forClass(UserOAuthAccount.class);
        verify(userOAuthAccountRepository).save(accountCaptor.capture());
        UserOAuthAccount savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getUser()).isSameAs(user);
        assertThat(savedAccount.getProvider()).isEqualTo(Provider.APPLE);
        assertThat(savedAccount.getProviderId()).isNull();
        assertThat(savedAccount.getOauthEmail()).isEqualTo("user@apple.com");
        assertThat(savedAccount.getAppleRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("linkPrimaryOAuthAccount는 복구 가능 기간 내 탈퇴 계정을 복구한다")
    void linkPrimaryOAuthAccountRestoresWithdrawnAccountWithinWindow() {
        // given
        User withdrawnUser = createWithdrawnUser(1, "2021136001", LocalDateTime.now().minusDays(2));
        UserOAuthAccount linkedAccount = UserOAuthAccount.of(
            withdrawnUser,
            Provider.GOOGLE,
            "google-id",
            "user@google.com",
            null
        );

        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.GOOGLE, "google-id"))
            .willReturn(Optional.of(linkedAccount));
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.GOOGLE, "google-id"))
            .willReturn(Optional.of(withdrawnUser));
        given(userOAuthAccountRepository.findAccountByProviderAndOauthEmail(Provider.GOOGLE, "user@google.com"))
            .willReturn(Optional.of(linkedAccount));
        given(userOAuthAccountRepository.findUserByOauthEmailAndProvider("user@google.com", Provider.GOOGLE))
            .willReturn(Optional.of(withdrawnUser));
        given(userOAuthAccountRepository.findByUserIdAndProvider(1, Provider.GOOGLE))
            .willReturn(Optional.of(linkedAccount));

        // when
        userOAuthAccountService.linkPrimaryOAuthAccount(
            withdrawnUser,
            Provider.GOOGLE,
            "google-id",
            "user@google.com",
            null
        );

        // then
        assertThat(withdrawnUser.getDeletedAt()).isNull();
        verify(userRepository).save(withdrawnUser);
        verify(userOAuthAccountRepository).save(linkedAccount);
        verify(userOAuthAccountRepository, never()).delete(linkedAccount);
    }

    @Test
    @DisplayName("linkOAuthAccount는 stage 환경에서는 복구 가능 기간 내 탈퇴 계정도 삭제한다")
    void linkOAuthAccountDeletesWithdrawnAccountInStageProfile() {
        // given
        User currentUser = createUser(1, "2021136001");
        User withdrawnUser = createWithdrawnUser(2, "2021136002", LocalDateTime.now().minusDays(1));
        UserOAuthAccount withdrawnAccount = UserOAuthAccount.of(
            withdrawnUser,
            Provider.GOOGLE,
            "google-id",
            "old@google.com",
            null
        );

        given(userRepository.getById(1)).willReturn(currentUser);
        given(environment.acceptsProfiles(any(Profiles.class))).willReturn(true);
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.GOOGLE, "google-id"))
            .willReturn(Optional.of(withdrawnAccount));
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.GOOGLE, "google-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findAccountByProviderAndOauthEmail(Provider.GOOGLE, "new@google.com"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByOauthEmailAndProvider("new@google.com", Provider.GOOGLE))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findByUserIdAndProvider(1, Provider.GOOGLE)).willReturn(Optional.empty());

        // when
        userOAuthAccountService.linkOAuthAccount(1, Provider.GOOGLE, "google-id", "new@google.com", null);

        // then
        verify(userOAuthAccountRepository).delete(withdrawnAccount);
        verify(userOAuthAccountRepository).flush();
        verify(userRepository, never()).save(withdrawnUser);
        verify(userOAuthAccountRepository).save(any(UserOAuthAccount.class));
    }

    @Test
    @DisplayName("linkOAuthAccount는 비어 있는 providerId를 기존 계정에 채운다")
    void linkOAuthAccountUpdatesExistingAccountProviderId() {
        // given
        User user = createUser(1, "2021136001");
        UserOAuthAccount account = UserOAuthAccount.of(user, Provider.APPLE, null, null, null);
        given(userRepository.getById(1)).willReturn(user);
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.APPLE, "apple-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.APPLE, "apple-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findAccountByProviderAndOauthEmail(Provider.APPLE, "user@apple.com"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByOauthEmailAndProvider("user@apple.com", Provider.APPLE))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findByUserIdAndProvider(1, Provider.APPLE)).willReturn(Optional.of(account));

        // when
        userOAuthAccountService.linkOAuthAccount(1, Provider.APPLE, "apple-id", "user@apple.com", "refresh-token");

        // then
        assertThat(account.getProviderId()).isEqualTo("apple-id");
        assertThat(account.getOauthEmail()).isEqualTo("user@apple.com");
        assertThat(account.getAppleRefreshToken()).isEqualTo("refresh-token");
        verify(userOAuthAccountRepository).save(account);
    }

    @Test
    @DisplayName("linkOAuthAccount는 다른 providerId가 이미 채워진 계정이면 충돌을 반환한다")
    void linkOAuthAccountRejectsConflictingProviderIdOnExistingAccount() {
        // given
        User user = createUser(1, "2021136001");
        UserOAuthAccount account = UserOAuthAccount.of(user, Provider.GOOGLE, "existing-id", "user@google.com", null);
        given(userRepository.getById(1)).willReturn(user);
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.GOOGLE, "new-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.GOOGLE, "new-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findAccountByProviderAndOauthEmail(Provider.GOOGLE, "user@google.com"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByOauthEmailAndProvider("user@google.com", Provider.GOOGLE))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findByUserIdAndProvider(1, Provider.GOOGLE)).willReturn(Optional.of(account));

        // when & then
        assertCustomExceptionCode(
            () -> userOAuthAccountService.linkOAuthAccount(1, Provider.GOOGLE, "new-id", "user@google.com", null),
            ApiResponseCode.OAUTH_PROVIDER_ALREADY_LINKED
        );
    }

    @Test
    @DisplayName("linkOAuthAccount는 다른 활성 사용자가 providerId를 점유하면 예외를 던진다")
    void linkOAuthAccountRejectsProviderOwnedByAnotherUser() {
        // given
        User currentUser = createUser(1, "2021136001");
        User linkedUser = createUser(2, "2021136002");
        given(userRepository.getById(1)).willReturn(currentUser);
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.GOOGLE, "google-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.GOOGLE, "google-id"))
            .willReturn(Optional.of(linkedUser));

        // when & then
        assertCustomExceptionCode(
            () -> userOAuthAccountService.linkOAuthAccount(1, Provider.GOOGLE, "google-id", null, null),
            ApiResponseCode.OAUTH_ACCOUNT_ALREADY_LINKED
        );
    }

    @Test
    @DisplayName("linkOAuthAccount는 다른 활성 사용자가 oauthEmail을 점유하면 예외를 던진다")
    void linkOAuthAccountRejectsOauthEmailOwnedByAnotherUser() {
        // given
        User currentUser = createUser(1, "2021136001");
        User linkedUser = createUser(2, "2021136002");
        given(userRepository.getById(1)).willReturn(currentUser);
        given(userOAuthAccountRepository.findAccountByProviderAndProviderId(Provider.APPLE, "apple-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByProviderAndProviderId(Provider.APPLE, "apple-id"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findAccountByProviderAndOauthEmail(Provider.APPLE, "owned@apple.com"))
            .willReturn(Optional.empty());
        given(userOAuthAccountRepository.findUserByOauthEmailAndProvider("owned@apple.com", Provider.APPLE))
            .willReturn(Optional.of(linkedUser));

        // when & then
        assertCustomExceptionCode(
            () -> userOAuthAccountService.linkOAuthAccount(1, Provider.APPLE, "apple-id", "owned@apple.com", null),
            ApiResponseCode.OAUTH_ACCOUNT_ALREADY_LINKED
        );
    }

    @Test
    @DisplayName("cleanupExpiredWithdrawnUserOAuthAccounts는 기준 시각으로 삭제 후 flush 한다")
    void cleanupExpiredWithdrawnUserOAuthAccountsDeletesAndFlushes() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 4, 10, 10, 0);
        given(userOAuthAccountRepository.deleteAllByWithdrawnUsersBefore(now.minusDays(7))).willReturn(3);

        // when
        int deletedCount = userOAuthAccountService.cleanupExpiredWithdrawnUserOAuthAccounts(now);

        // then
        assertThat(deletedCount).isEqualTo(3);
        verify(userOAuthAccountRepository).deleteAllByWithdrawnUsersBefore(now.minusDays(7));
        verify(userOAuthAccountRepository).flush();
    }

    @Test
    @DisplayName("getPrimaryOAuthAccount는 첫 번째 계정을 반환하고 없으면 null을 반환한다")
    void getPrimaryOAuthAccountReturnsFirstAccountOrNull() {
        // given
        User user = createUser(1, "2021136001");
        UserOAuthAccount first = UserOAuthAccount.of(user, Provider.GOOGLE, "google-id", "user@google.com", null);
        UserOAuthAccount second = UserOAuthAccount.of(user, Provider.APPLE, "apple-id", "user@apple.com", "refresh");
        given(userOAuthAccountRepository.findAllByUserId(1)).willReturn(List.of(first, second));
        given(userOAuthAccountRepository.findAllByUserId(2)).willReturn(List.of());

        // when & then
        assertThat(userOAuthAccountService.getPrimaryOAuthAccount(1)).isSameAs(first);
        assertThat(userOAuthAccountService.getPrimaryOAuthAccount(2)).isNull();
    }

    private User createUser(Integer id, String studentNumber) {
        University university = UniversityFixture.create();
        return User.builder()
            .id(id)
            .university(university)
            .email(studentNumber + "@koreatech.ac.kr")
            .name("테스트유저" + id)
            .studentNumber(studentNumber)
            .isMarketingAgreement(true)
            .imageUrl("https://example.com/profile-" + id + ".png")
            .build();
    }

    private User createWithdrawnUser(Integer id, String studentNumber, LocalDateTime deletedAt) {
        User user = createUser(id, studentNumber);
        user.withdraw(deletedAt);
        return user;
    }

    private void assertCustomExceptionCode(ThrowingCallable callable, ApiResponseCode expectedCode) {
        assertThatThrownBy(callable::call)
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode()).isEqualTo(expectedCode));
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }
}
