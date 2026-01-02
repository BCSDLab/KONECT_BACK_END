package gg.agit.konect.domain.studytime.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.studytime.dto.StudyTimeSummaryResponse;
import gg.agit.konect.domain.studytime.model.StudyTimeDaily;
import gg.agit.konect.domain.studytime.model.StudyTimeMonthly;
import gg.agit.konect.domain.studytime.model.StudyTimeTotal;
import gg.agit.konect.domain.studytime.repository.StudyTimeDailyRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeMonthlyRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeTotalRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyTimeQueryService {

    private final StudyTimeDailyRepository studyTimeDailyRepository;
    private final StudyTimeMonthlyRepository studyTimeMonthlyRepository;
    private final StudyTimeTotalRepository studyTimeTotalRepository;

    public StudyTimeSummaryResponse getSummary(Integer userId) {
        Long dailyStudyTime = getDailyStudyTime(userId);
        Long monthlyStudyTime = getMonthlyStudyTime(userId);
        Long totalStudyTime = getTotalStudyTime(userId);

        return StudyTimeSummaryResponse.of(dailyStudyTime, monthlyStudyTime, totalStudyTime);
    }

    public long getTotalStudyTime(Integer userId) {
        return studyTimeTotalRepository.findByUserId(userId)
            .map(StudyTimeTotal::getTotalSeconds)
            .orElse(0L);
    }

    public long getDailyStudyTime(Integer userId) {
        LocalDate today = LocalDate.now();

        return studyTimeDailyRepository.findByUserIdAndStudyDate(userId, today)
            .map(StudyTimeDaily::getTotalSeconds)
            .orElse(0L);
    }

    public long getMonthlyStudyTime(Integer userId) {
        LocalDate month = LocalDate.now().withDayOfMonth(1);

        return studyTimeMonthlyRepository.findByUserIdAndStudyMonth(userId, month)
            .map(StudyTimeMonthly::getTotalSeconds)
            .orElse(0L);
    }
}
