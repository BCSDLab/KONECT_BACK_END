package gg.agit.konect.domain.studytime.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.studytime.repository.StudyTimeRankingRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyTimeSchedulerService {

    private final StudyTimeRankingRepository studyTimeRankingRepository;

    @Transactional
    public int resetStudyTimeRanking(LocalDate targetDate) {
        if (targetDate.getDayOfMonth() == 1) {
            return studyTimeRankingRepository.resetDailyAndMonthlySeconds();
        }
        return studyTimeRankingRepository.resetDailySeconds();
    }

    @Transactional
    public int resetStudyTimeRankingDaily() {
        return studyTimeRankingRepository.resetDailySeconds();
    }

    @Transactional
    public int resetStudyTimeRankingMonthly() {
        return studyTimeRankingRepository.resetDailyAndMonthlySeconds();
    }
}
