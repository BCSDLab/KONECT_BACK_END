package gg.agit.konect.domain.user.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.model.UserOAuthAccount;
import gg.agit.konect.infrastructure.oauth.AppleTokenRevocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSchedulerService {

    private static final int RESTORE_WINDOW_DAYS = 7;

    private final UserSchedulerTxService userSchedulerTxService;
    private final UserOAuthAccountService userOAuthAccountService;
    private final AppleTokenRevocationService appleTokenRevocationService;

    public void cleanupExpiredWithdrawnOAuthAccountsAfterRestoreWindow() {
        cleanupExpiredWithdrawnOAuthAccountsAfterRestoreWindow(LocalDateTime.now());
    }

    public void cleanupExpiredWithdrawnOAuthAccountsAfterRestoreWindow(LocalDateTime now) {
        LocalDateTime threshold = now.minusDays(RESTORE_WINDOW_DAYS);
        List<UserOAuthAccount> accountsToRevoke = userSchedulerTxService.findAccountsToRevoke(threshold);

        int successCount = 0;
        int failureCount = 0;

        for (UserOAuthAccount account : accountsToRevoke) {
            try {
                String refreshToken = account.getAppleRefreshToken();

                appleTokenRevocationService.revoke(refreshToken);
                userSchedulerTxService.clearAppleRefreshTokenIfMatches(account.getId(), refreshToken);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                User user = account.getUser();
                log.error("Failed to revoke Apple token for userId={}, accountId={}", user.getId(), account.getId(), e);
            }
        }

        int deletedCount = userOAuthAccountService.cleanupExpiredWithdrawnUserOAuthAccounts(now);

        log.info(
            "Expired withdrawn OAuth cleanup task finished: revokeTotal={}, revokeSuccess={}, revokeFailure={}, "
                + "deleted={}",
            accountsToRevoke.size(),
            successCount,
            failureCount,
            deletedCount
        );
    }
}
