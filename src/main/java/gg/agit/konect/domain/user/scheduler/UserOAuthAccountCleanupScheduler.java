package gg.agit.konect.domain.user.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import gg.agit.konect.domain.user.service.UserOAuthAccountService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserOAuthAccountCleanupScheduler {

    private static final Logger SCHEDULER_LOGGER = LoggerFactory.getLogger("scheduler.user-oauth-account");

    private final UserOAuthAccountService userOAuthAccountService;

    @Scheduled(cron = "0 10 0 * * *")
    public void cleanupExpiredWithdrawnUserOAuthAccounts() {
        try {
            int deletedCount = userOAuthAccountService.cleanupExpiredWithdrawnUserOAuthAccounts();
            SCHEDULER_LOGGER.info("탈퇴 유예기간 경과 OAuth 계정 정리 완료. deletedCount={}", deletedCount);
        } catch (Exception e) {
            SCHEDULER_LOGGER.error("탈퇴 유예기간 경과 OAuth 계정 정리 중 오류가 발생했습니다.", e);
        }
    }
}
