package gg.agit.konect.domain.user.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import gg.agit.konect.domain.user.model.UserOAuthAccount;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.infrastructure.oauth.AppleTokenRevocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSchedulerService {

    private static final int REVOKE_AFTER_DAYS = 7;

    private final UserSchedulerTxService userSchedulerTxService;
    private final AppleTokenRevocationService appleTokenRevocationService;

    /**
     * 7일 이상 경과한 Apple 사용자의 토큰을 revoke합니다.
     * - 7일 복구 정책: 탈퇴 후 7일 이내 복구 가능하므로 즉시 revoke하지 않음
     * - 7일 경과 후: 복구 불가 시점이므로 Apple 토큰 영구 폐기
     */
    public void revokeAppleTokensAfterRestoreWindow() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(REVOKE_AFTER_DAYS);
        List<UserOAuthAccount> accountsToRevoke = userSchedulerTxService.findAccountsToRevoke(threshold);

        if (accountsToRevoke.isEmpty()) {
            log.info("No Apple users to revoke (threshold={})", threshold);
            return;
        }

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

        log.info(
            "Apple token revoke task finished: total={}, success={}, failure={}"
            , accountsToRevoke.size(), successCount, failureCount
        );
    }
}
