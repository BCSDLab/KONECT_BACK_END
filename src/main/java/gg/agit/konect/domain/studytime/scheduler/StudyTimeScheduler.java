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
    public void updateClubStudyTimeRanking() {
        try {
            log.info("동아리 공부 시간 랭킹 업데이트 시작");
            studyTimeSchedulerService.updateClubStudyTimeRanking();
            log.info("동아리 공부 시간 랭킹 업데이트 완료");
        } catch (Exception e) {
            log.error("동아리 공부 시간 랭킹 업데이트 과정에서 오류가 발생했습니다.", e);
        }
    }

    @Scheduled(fixedDelay = 5, timeUnit = MINUTES)
    public void updatePersonalStudyTimeRanking() {
        try {
            log.info("개인 공부 시간 랭킹 업데이트 시작");
            studyTimeSchedulerService.updatePersonalStudyTimeRanking();
            log.info("개인 공부 시간 랭킹 업데이트 완료");
        } catch (Exception e) {
            log.error("개인 공부 시간 랭킹 업데이트 과정에서 오류가 발생했습니다.", e);
        }
    }

    @Scheduled(fixedDelay = 5, timeUnit = MINUTES)
    public void updateStudentNumberStudyTimeRanking() {
        try {
            log.info("학번별 공부 시간 랭킹 업데이트 시작");
            studyTimeSchedulerService.updateStudentNumberStudyTimeRanking();
            log.info("학번별 공부 시간 랭킹 업데이트 완료");
        } catch (Exception e) {
            log.error("학번별 공부 시간 랭킹 업데이트 과정에서 오류가 발생했습니다.", e);
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void resetStudyTimeRankingDaily() {
        try {
            log.info("일일 공부 시간 랭킹 초기화 시작");
            studyTimeSchedulerService.resetStudyTimeRankingDaily();
            log.info("일일 공부 시간 랭킹 초기화 완료");
        } catch (Exception e) {
            log.error("일일 공부 시간 랭킹 초기화 과정에서 오류가 발생했습니다.", e);
        }
    }

    @Scheduled(cron = "0 0 0 1 * *")
    public void resetStudyTimeRankingMonthly() {
        try {
            log.info("월간 공부 시간 랭킹 초기화 시작");
            studyTimeSchedulerService.resetStudyTimeRankingMonthly();
            log.info("월간 공부 시간 랭킹 초기화 완료");
        } catch (Exception e) {
            log.error("월간 공부 시간 랭킹 초기화 과정에서 오류가 발생했습니다.", e);
        }
    }
}
