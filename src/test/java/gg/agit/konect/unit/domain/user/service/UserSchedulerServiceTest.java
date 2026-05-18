package gg.agit.konect.unit.domain.user.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.model.UserOAuthAccount;
import gg.agit.konect.domain.user.service.UserOAuthAccountService;
import gg.agit.konect.domain.user.service.UserSchedulerService;
import gg.agit.konect.domain.user.service.UserSchedulerTxService;
import gg.agit.konect.infrastructure.oauth.AppleTokenRevocationService;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.UserFixture;

class UserSchedulerServiceTest extends ServiceTestSupport {

    @Mock
    private UserSchedulerTxService userSchedulerTxService;

    @Mock
    private UserOAuthAccountService userOAuthAccountService;

    @Mock
    private AppleTokenRevocationService appleTokenRevocationService;

    @InjectMocks
    private UserSchedulerService userSchedulerService;

    @Test
    @DisplayName("Apple 토큰 revoke 후 같은 실행에서 만료 OAuth 계정을 정리한다")
    void cleanupExpiredWithdrawnOAuthAccountsRevokesAppleTokensBeforeCleanup() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 4, 24, 0, 10);
        User user = UserFixture.createWithdrawnUser(1, "2021136001", now.minusDays(8));
        UserOAuthAccount account = UserOAuthAccount.of(
            user,
            Provider.APPLE,
            "apple-provider-id",
            "apple@konect.test",
            "apple-refresh-token"
        );
        given(userSchedulerTxService.findAccountsToRevoke(now.minusDays(7))).willReturn(List.of(account));
        given(userOAuthAccountService.cleanupExpiredWithdrawnUserOAuthAccounts(now)).willReturn(1);

        // when
        userSchedulerService.cleanupExpiredWithdrawnOAuthAccountsAfterRestoreWindow(now);

        // then
        InOrder inOrder = inOrder(appleTokenRevocationService, userSchedulerTxService, userOAuthAccountService);
        inOrder.verify(appleTokenRevocationService).revoke("apple-refresh-token");
        inOrder.verify(userSchedulerTxService).clearAppleRefreshTokenIfMatches(account.getId(), "apple-refresh-token");
        inOrder.verify(userOAuthAccountService).cleanupExpiredWithdrawnUserOAuthAccounts(now);
    }

    @Test
    @DisplayName("Apple 토큰 revoke가 실패해도 정리는 진행하되 토큰 제거는 하지 않는다")
    void cleanupExpiredWithdrawnOAuthAccountsKeepsFailedAppleTokenForRetry() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 4, 24, 0, 10);
        User user = UserFixture.createWithdrawnUser(1, "2021136001", now.minusDays(8));
        UserOAuthAccount account = UserOAuthAccount.of(
            user,
            Provider.APPLE,
            "apple-provider-id",
            "apple@konect.test",
            "apple-refresh-token"
        );
        given(userSchedulerTxService.findAccountsToRevoke(now.minusDays(7))).willReturn(List.of(account));
        given(userOAuthAccountService.cleanupExpiredWithdrawnUserOAuthAccounts(now)).willReturn(0);
        willThrow(new IllegalStateException("Apple revoke failed"))
            .given(appleTokenRevocationService).revoke("apple-refresh-token");

        // when
        userSchedulerService.cleanupExpiredWithdrawnOAuthAccountsAfterRestoreWindow(now);

        // then
        verify(userSchedulerTxService, never()).clearAppleRefreshTokenIfMatches(account.getId(), "apple-refresh-token");
        verify(userOAuthAccountService).cleanupExpiredWithdrawnUserOAuthAccounts(now);
    }
}
