package gg.agit.konect.domain.studytime.scheduler;

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
    public void resetStudyTimeRankingDaily() {
        try {
            SCHEDULER_LOGGER.info("일일 공부 시간 랭킹 초기화 시작");
            studyTimeSchedulerService.resetStudyTimeRankingDaily();
            SCHEDULER_LOGGER.info("일일 공부 시간 랭킹 초기화 완료");
        } catch (Exception e) {
            SCHEDULER_LOGGER.error("일일 공부 시간 랭킹 초기화 과정에서 오류가 발생했습니다.", e);
        }
    }

    @Scheduled(cron = "0 0 0 1 * *")
    public void resetStudyTimeRankingMonthly() {
        try {
            SCHEDULER_LOGGER.info("월간 공부 시간 랭킹 초기화 시작");
            studyTimeSchedulerService.resetStudyTimeRankingMonthly();
            SCHEDULER_LOGGER.info("월간 공부 시간 랭킹 초기화 완료");
        } catch (Exception e) {
            SCHEDULER_LOGGER.error("월간 공부 시간 랭킹 초기화 과정에서 오류가 발생했습니다.", e);
        }
    }
}
