package gg.agit.konect.domain.user.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.user.model.WithdrawnUser;
import gg.agit.konect.domain.user.repository.WithdrawnUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawnUserCleanupScheduler {

    private final WithdrawnUserRepository withdrawnUserRepository;

    // 매일 새벽 2시 실행
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredWithdrawnUsers() {
        log.info("Starting cleanup of expired withdrawn users");

        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        List<WithdrawnUser> expiredUsers = withdrawnUserRepository.findByWithdrawnAtBefore(oneYearAgo);

        if (expiredUsers.isEmpty()) {
            log.info("No expired withdrawn users found");
            return;
        }

        int count = expiredUsers.size();
        withdrawnUserRepository.deleteAll(expiredUsers);

        log.info("Deleted {} expired withdrawn users (withdrawn before: {})", count, oneYearAgo);
    }
}
