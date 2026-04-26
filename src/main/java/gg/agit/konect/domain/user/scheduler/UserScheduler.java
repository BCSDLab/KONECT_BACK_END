package gg.agit.konect.domain.user.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import gg.agit.konect.domain.user.service.UserSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserScheduler {

    private final UserSchedulerService userSchedulerService;

    @Scheduled(cron = "0 10 0 * * *")
    public void cleanupExpiredWithdrawnOAuthAccountsAfterRestoreWindow() {
        try {
            log.info("Starting expired withdrawn OAuth cleanup task");
            userSchedulerService.cleanupExpiredWithdrawnOAuthAccountsAfterRestoreWindow();
            log.info("Successfully completed expired withdrawn OAuth cleanup task");
        } catch (Exception e) {
            log.error("Failed to cleanup expired withdrawn OAuth accounts", e);
        }
    }
}
