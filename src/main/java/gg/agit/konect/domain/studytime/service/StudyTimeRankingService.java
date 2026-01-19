package gg.agit.konect.domain.studytime.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.studytime.dto.StudyTimeMyRankingCondition;
import gg.agit.konect.domain.studytime.dto.StudyTimeMyRankingsResponse;
import gg.agit.konect.domain.studytime.dto.StudyTimeRankingCondition;
import gg.agit.konect.domain.studytime.dto.StudyTimeRankingResponse;
import gg.agit.konect.domain.studytime.dto.StudyTimeRankingsResponse;
import gg.agit.konect.domain.studytime.enums.StudyTimeRankingSort;
import gg.agit.konect.domain.studytime.model.RankingType;
import gg.agit.konect.domain.studytime.model.StudyTimeRanking;
import gg.agit.konect.domain.studytime.model.StudyTimeRankingId;
import gg.agit.konect.domain.studytime.repository.RankingTypeRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeRankingRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyTimeRankingService {

    private static final String RANKING_TYPE_CLUB = "CLUB";
    private static final String RANKING_TYPE_STUDENT_NUMBER = "STUDENT_NUMBER";
    private static final String RANKING_TYPE_PERSONAL = "PERSONAL";

    private final StudyTimeRankingRepository studyTimeRankingRepository;
    private final RankingTypeRepository rankingTypeRepository;
    private final UserRepository userRepository;
    private final ClubMemberRepository clubMemberRepository;

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

    public StudyTimeMyRankingsResponse getMyRankings(StudyTimeMyRankingCondition condition, Integer userId) {
        User user = userRepository.getById(userId);
        Integer universityId = user.getUniversity().getId();
        StudyTimeRankingSort sort = condition.sort();

        Integer clubRankingTypeId = rankingTypeRepository.getByNameIgnoreCase(RANKING_TYPE_CLUB).getId();
        Integer studentRankingTypeId = rankingTypeRepository.getByNameIgnoreCase(RANKING_TYPE_STUDENT_NUMBER).getId();
        Integer personalRankingTypeId = rankingTypeRepository.getByNameIgnoreCase(RANKING_TYPE_PERSONAL).getId();

        List<StudyTimeRankingResponse> clubRankings = resolveClubRankings(
            clubRankingTypeId,
            universityId,
            sort,
            userId
        );

        StudyTimeRankingResponse studentNumberRanking = resolveStudentNumberRanking(
            studentRankingTypeId,
            universityId,
            sort,
            user.getStudentNumber()
        );

        StudyTimeRankingResponse personalRanking = resolveRanking(
            personalRankingTypeId,
            universityId,
            sort,
            userId
        );

        return StudyTimeMyRankingsResponse.of(
            clubRankings,
            studentNumberRanking,
            personalRanking
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

    private List<StudyTimeRankingResponse> resolveClubRankings(
        Integer rankingTypeId,
        Integer universityId,
        StudyTimeRankingSort sort,
        Integer userId
    ) {
        List<ClubMember> clubMembers = clubMemberRepository.findByUserId(userId);

        return clubMembers.stream()
            .map(member -> resolveRanking(rankingTypeId, universityId, sort, member.getId().getClubId()))
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(StudyTimeRankingResponse::rank))
            .toList();
    }

    private StudyTimeRankingResponse resolveStudentNumberRanking(
        Integer rankingTypeId,
        Integer universityId,
        StudyTimeRankingSort sort,
        String studentNumber
    ) {
        String targetName = resolveStudentNumber(studentNumber);

        return resolveRankingByName(rankingTypeId, universityId, sort, targetName);
    }

    private StudyTimeRankingResponse resolveRanking(
        Integer rankingTypeId,
        Integer universityId,
        StudyTimeRankingSort sort,
        Integer targetId
    ) {
        return studyTimeRankingRepository.findRanking(rankingTypeId, universityId, targetId)
            .map(ranking -> StudyTimeRankingResponse.from(ranking, calculateRank(ranking, sort)))
            .orElse(null);
    }

    private StudyTimeRankingResponse resolveRankingByName(
        Integer rankingTypeId,
        Integer universityId,
        StudyTimeRankingSort sort,
        String targetName
    ) {
        return studyTimeRankingRepository.findRankingByName(rankingTypeId, universityId, targetName)
            .map(ranking -> StudyTimeRankingResponse.from(ranking, calculateRank(ranking, sort)))
            .orElse(null);
    }

    private int calculateRank(StudyTimeRanking ranking, StudyTimeRankingSort sort) {
        StudyTimeRankingId rankingId = ranking.getId();
        long higherCount;

        if (sort == StudyTimeRankingSort.DAILY) {
            higherCount = studyTimeRankingRepository.countDailyHigherRankings(
                rankingId.getRankingTypeId(),
                rankingId.getUniversityId(),
                ranking.getDailySeconds(),
                ranking.getMonthlySeconds(),
                rankingId.getTargetId()
            );
        } else {
            higherCount = studyTimeRankingRepository.countMonthlyHigherRankings(
                rankingId.getRankingTypeId(),
                rankingId.getUniversityId(),
                ranking.getMonthlySeconds(),
                ranking.getDailySeconds(),
                rankingId.getTargetId()
            );
        }

        return (int)higherCount + 1;
    }

    private String resolveStudentNumber(String studentNumber) {
        return String.valueOf(Integer.parseInt(studentNumber.substring(2, 4)));
    }
}
