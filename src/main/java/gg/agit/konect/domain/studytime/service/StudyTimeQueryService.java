package gg.agit.konect.domain.studytime.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.studytime.dto.StudyTimeSummaryResponse;
import gg.agit.konect.domain.studytime.model.StudyTimeDaily;
import gg.agit.konect.domain.studytime.model.StudyTimeTotal;
import gg.agit.konect.domain.studytime.repository.StudyTimeDailyRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeTotalRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyTimeQueryService {

    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_MINUTE = 60;

    private final StudyTimeDailyRepository studyTimeDailyRepository;
    private final StudyTimeTotalRepository studyTimeTotalRepository;

    public StudyTimeSummaryResponse getSummary(Integer userId) {
        String todayStudyTime = getTodayStudyTime(userId);
        String totalStudyTime = getTotalStudyTime(userId);

        return StudyTimeSummaryResponse.of(todayStudyTime, totalStudyTime);
    }

    public String getTotalStudyTime(Integer userId) {
        long totalSeconds = studyTimeTotalRepository.findByUserId(userId)
            .map(StudyTimeTotal::getTotalSeconds)
            .orElse(0L);

        return formatSeconds(totalSeconds);
    }

    public String getTodayStudyTime(Integer userId) {
        LocalDate today = LocalDate.now();
        long todaySeconds = studyTimeDailyRepository.findByUserIdAndStudyDate(userId, today)
            .map(StudyTimeDaily::getTotalSeconds)
            .orElse(0L);

        return formatSeconds(todaySeconds);
    }

    private String formatSeconds(long seconds) {
        long hours = seconds / SECONDS_PER_HOUR;
        long minutes = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
        long remainingSeconds = seconds % SECONDS_PER_MINUTE;

        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }
}
