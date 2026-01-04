package gg.agit.konect.domain.studytime.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.studytime.dto.StudyTimeRankingCondition;
import gg.agit.konect.domain.studytime.dto.StudyTimeRankingsResponse;
import gg.agit.konect.domain.studytime.dto.StudyTimeRankingsResponse.InnerStudyTimeRanking;
import gg.agit.konect.domain.studytime.enums.StudyTimeRankingSort;
import gg.agit.konect.domain.studytime.enums.StudyTimeRankingType;
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

        List<InnerStudyTimeRanking> rankings = toResponse(
            rankingsBaseRank(page, limit),
            rankingPage.getContent(),
            condition.type()
        );

        return StudyTimeRankingsResponse.of(
            rankingPage.getTotalElements(),
            rankingPage.getNumberOfElements(),
            rankingPage.getTotalPages(),
            rankingPage.getNumber() + 1,
            rankings
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

    private List<InnerStudyTimeRanking> toResponse(
        int baseRank,
        List<StudyTimeRanking> rankings,
        StudyTimeRankingType type
    ) {
        AtomicInteger currentRank = new AtomicInteger(baseRank);

        return rankings.stream()
            .map(ranking -> new InnerStudyTimeRanking(
                currentRank.getAndIncrement(),
                resolveName(ranking, type),
                ranking.getMonthlySeconds(),
                ranking.getDailySeconds()
            ))
            .toList();
    }

    private String resolveName(StudyTimeRanking ranking, StudyTimeRankingType type) {
        String name = ranking.getTargetName();

        if (type == StudyTimeRankingType.PERSONAL) {
            return maskPersonalName(name);
        }

        return name;
    }

    private String maskPersonalName(String name) {
        if (name.length() == 1) {
            return name;
        }

        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }

        return name.charAt(0)
            + "*".repeat(name.length() - 2)
            + name.substring(name.length() - 1);
    }
}
