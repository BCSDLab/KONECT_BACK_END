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

    /**
     * 매일 자정(서버 기본 시간대 기준 00:00)에 실행되어 7일 경과한 Apple 사용자 토큰을 revoke합니다.
     * cron 표현식: 초 분 시 일 월 요일
     * 0 0 0 * * *: 매일 00:00:00 실행
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void revokeAppleTokensAfterRestoreWindow() {
        try {
            log.info("Starting Apple token revocation task for users withdrawn more than 7 days ago");
            userSchedulerService.revokeAppleTokensAfterRestoreWindow();
            log.info("Successfully completed Apple token revocation task");
        } catch (Exception e) {
            log.error("Failed to revoke Apple tokens for withdrawn users", e);
        }
    }
}
