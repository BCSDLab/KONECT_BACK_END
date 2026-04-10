package gg.agit.konect.domain.studytime.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.studytime.model.StudyTimeRanking;
import gg.agit.konect.domain.studytime.repository.StudyTimeRankingRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyTimeSchedulerService {

    private final StudyTimeRankingRepository studyTimeRankingRepository;

    @Transactional
    public void resetStudyTimeRankingDaily() {
        List<StudyTimeRanking> studyTimeRankings = studyTimeRankingRepository.findAll();
        studyTimeRankings.forEach(ranking -> ranking.updateSeconds(0L, ranking.getMonthlySeconds()));
    }

    @Transactional
    public void resetStudyTimeRankingMonthly() {
        List<StudyTimeRanking> studyTimeRankings = studyTimeRankingRepository.findAll();
        studyTimeRankings.forEach(ranking -> ranking.updateSeconds(ranking.getDailySeconds(), 0L));
    }
}
