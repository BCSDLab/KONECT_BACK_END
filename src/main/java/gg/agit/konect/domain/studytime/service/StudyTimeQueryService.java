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

    private final StudyTimeDailyRepository studyTimeDailyRepository;
    private final StudyTimeTotalRepository studyTimeTotalRepository;

    public StudyTimeSummaryResponse getSummary(Integer userId) {
        Long todayStudyTime = getTodayStudyTime(userId);
        Long totalStudyTime = getTotalStudyTime(userId);

        return StudyTimeSummaryResponse.of(todayStudyTime, totalStudyTime);
    }

    public long getTotalStudyTime(Integer userId) {
        return studyTimeTotalRepository.findByUserId(userId)
            .map(StudyTimeTotal::getTotalSeconds)
            .orElse(0L);
    }

    public long getTodayStudyTime(Integer userId) {
        LocalDate today = LocalDate.now();

        return studyTimeDailyRepository.findByUserIdAndStudyDate(userId, today)
            .map(StudyTimeDaily::getTotalSeconds)
            .orElse(0L);

    }
}
