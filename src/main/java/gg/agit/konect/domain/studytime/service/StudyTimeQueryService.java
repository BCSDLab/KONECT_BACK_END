package gg.agit.konect.domain.studytime.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.studytime.dto.StudyTimeSummaryResponse;
import gg.agit.konect.domain.studytime.model.StudyTimeDaily;
import gg.agit.konect.domain.studytime.repository.StudyTimeDailyRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyTimeQueryService {

    private final StudyTimeDailyRepository studyTimeDailyRepository;

    public StudyTimeSummaryResponse getSummary(Integer userId) {
        Long dailyStudyTime = getDailyStudyTime(userId);
        Long monthlyStudyTime = getMonthlyStudyTime(userId);
        Long totalStudyTime = getTotalStudyTime(userId);

        return StudyTimeSummaryResponse.of(dailyStudyTime, monthlyStudyTime, totalStudyTime);
    }

    public long getTotalStudyTime(Integer userId) {
        return studyTimeDailyRepository.sumTotalSecondsByUserId(userId);
    }

    public long getDailyStudyTime(Integer userId) {
        LocalDate today = LocalDate.now();

        return studyTimeDailyRepository.findByUserIdAndStudyDate(userId, today)
            .map(StudyTimeDaily::getTotalSeconds)
            .orElse(0L);
    }

    public long getMonthlyStudyTime(Integer userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        return studyTimeDailyRepository.sumTotalSecondsByUserIdAndStudyDateBetween(userId, monthStart, today);
    }
}
