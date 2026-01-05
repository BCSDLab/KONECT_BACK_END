package gg.agit.konect.domain.studytime.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.studytime.dto.StudyTimeRankingCondition;
import gg.agit.konect.domain.studytime.dto.StudyTimeRankingsResponse;
import gg.agit.konect.domain.studytime.enums.StudyTimeRankingSort;
import gg.agit.konect.domain.studytime.model.StudyTimeRanking;
import gg.agit.konect.domain.studytime.repository.StudyTimeRankingRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyTimeRankingService {

    private final StudyTimeRankingRepository studyTimeRankingRepository;

    public StudyTimeRankingsResponse getRankings(StudyTimeRankingCondition condition) {
        int page = condition.page();
        int limit = condition.limit();

        PageRequest pageable = PageRequest.of(page - 1, limit);
        Page<StudyTimeRanking> rankingPage = fetchRankings(condition, pageable);

        return StudyTimeRankingsResponse.from(
            rankingPage,
            rankingsBaseRank(page, limit),
            condition.type()
        );
    }

    private Page<StudyTimeRanking> fetchRankings(StudyTimeRankingCondition condition, PageRequest pageable) {
        StudyTimeRankingSort sort = condition.sort();

        if (sort == StudyTimeRankingSort.DAILY) {
            return studyTimeRankingRepository.findDailyRankings(condition.type(), pageable);
        }

        return studyTimeRankingRepository.findMonthlyRankings(condition.type(), pageable);
    }

    private int rankingsBaseRank(int page, int limit) {
        return (page - 1) * limit + 1;
    }
}
