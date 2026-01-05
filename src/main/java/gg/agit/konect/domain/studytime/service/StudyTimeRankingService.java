package gg.agit.konect.domain.studytime.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.studytime.dto.StudyTimeRankingCondition;
import gg.agit.konect.domain.studytime.dto.StudyTimeRankingsResponse;
import gg.agit.konect.domain.studytime.enums.StudyTimeRankingSort;
import gg.agit.konect.domain.studytime.model.RankingType;
import gg.agit.konect.domain.studytime.model.StudyTimeRanking;
import gg.agit.konect.domain.studytime.repository.RankingTypeRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeRankingRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyTimeRankingService {

    private final StudyTimeRankingRepository studyTimeRankingRepository;
    private final RankingTypeRepository rankingTypeRepository;
    private final UserRepository userRepository;

    public StudyTimeRankingsResponse getRankings(StudyTimeRankingCondition condition, Integer userId) {
        int page = condition.page();
        int limit = condition.limit();
        String rankingTypeName = condition.type().trim();

        User user = userRepository.getById(userId);
        RankingType rankingType = rankingTypeRepository.getByNameIgnoreCase(rankingTypeName);

        PageRequest pageable = PageRequest.of(page - 1, limit);
        Page<StudyTimeRanking> rankingPage = fetchRankings(
            condition,
            pageable,
            rankingType,
            user.getUniversity().getId()
        );

        return StudyTimeRankingsResponse.from(
            rankingPage,
            rankingsBaseRank(page, limit),
            rankingTypeName
        );
    }

    private Page<StudyTimeRanking> fetchRankings(
        StudyTimeRankingCondition condition,
        PageRequest pageable,
        RankingType rankingType,
        Integer universityId
    ) {
        StudyTimeRankingSort sort = condition.sort();
        Integer rankingTypeId = rankingType.getId();

        if (sort == StudyTimeRankingSort.DAILY) {
            return studyTimeRankingRepository.findDailyRankings(rankingTypeId, universityId, pageable);
        }

        return studyTimeRankingRepository.findMonthlyRankings(rankingTypeId, universityId, pageable);
    }

    private int rankingsBaseRank(int page, int limit) {
        return (page - 1) * limit + 1;
    }
}
