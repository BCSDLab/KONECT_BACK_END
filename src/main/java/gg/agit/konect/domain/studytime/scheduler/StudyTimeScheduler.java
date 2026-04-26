package gg.agit.konect.domain.studytime.scheduler;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import gg.agit.konect.domain.studytime.service.StudyTimeSchedulerService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StudyTimeScheduler {

    private static final Logger SCHEDULER_LOGGER = LoggerFactory.getLogger("scheduler.studytime");

    private final StudyTimeSchedulerService studyTimeSchedulerService;

    @Scheduled(cron = "0 0 0 * * *")
    public void resetStudyTimeRanking() {
        try {
            LocalDate today = LocalDate.now();
            SCHEDULER_LOGGER.info("스터디 시간 랭킹 초기화를 시작합니다. targetDate={}", today);
            int updatedCount = studyTimeSchedulerService.resetStudyTimeRanking(today);
            SCHEDULER_LOGGER.info("스터디 시간 랭킹 초기화를 완료했습니다. targetDate={}, updatedCount={}", today, updatedCount);
        } catch (Exception e) {
            SCHEDULER_LOGGER.error("스터디 시간 랭킹 초기화 중 오류가 발생했습니다.", e);
        }
    }
}
