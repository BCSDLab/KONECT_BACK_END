package gg.agit.konect.domain.studytime.scheduler;

import static java.util.concurrent.TimeUnit.MINUTES;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import gg.agit.konect.domain.studytime.service.StudyTimeSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyTimeScheduler {

    private final StudyTimeSchedulerService studyTimeSchedulerService;

    @Scheduled(fixedDelay = 5, timeUnit = MINUTES)
    public void clubStudyTimeRankingUpdate() {
        try {
            studyTimeSchedulerService.updateClubStudyTimeRanking();
        } catch (Exception e) {
            log.error("동아리 공부 시간 랭킹 업데이트 과정에서 오류가 발생했습니다.", e);
        }
    }
}
