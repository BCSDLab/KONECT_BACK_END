package gg.agit.konect.domain.studytime.service;

import static gg.agit.konect.domain.studytime.model.RankingType.RANKING_TYPE_CLUB;
import static java.util.stream.Collectors.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubMemberId;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.studytime.model.RankingType;
import gg.agit.konect.domain.studytime.model.StudyTimeDaily;
import gg.agit.konect.domain.studytime.model.StudyTimeMonthly;
import gg.agit.konect.domain.studytime.model.StudyTimeRanking;
import gg.agit.konect.domain.studytime.model.StudyTimeRankingId;
import gg.agit.konect.domain.studytime.repository.RankingTypeRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeDailyRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeMonthlyRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeRankingRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyTimeSchedulerService {

    private final ClubMemberRepository clubMemberRepository;
    private final StudyTimeDailyRepository studyTimeDailyRepository;
    private final StudyTimeMonthlyRepository studyTimeMonthlyRepository;
    private final StudyTimeRankingRepository studyTimeRankingRepository;
    private final RankingTypeRepository rankingTypeRepository;

    @Transactional
    public void updateClubStudyTimeRanking() {
        RankingType rankingType = rankingTypeRepository.getByNameIgnoreCase(RANKING_TYPE_CLUB);
        List<StudyTimeRanking> studyTimeRankings = studyTimeRankingRepository.findByRankingTypeId(rankingType.getId());
        if (studyTimeRankings.isEmpty()) {
            return ;
        }

        List<Integer> clubIds = studyTimeRankings.stream()
            .map(StudyTimeRanking::getId)
            .map(StudyTimeRankingId::getTargetId)
            .toList();

        List<ClubMember> clubMembers = clubMemberRepository.findByClubIdIn(clubIds);

        List<Integer> userIds = clubMembers.stream()
            .map(ClubMember::getId)
            .map(ClubMemberId::getUserId)
            .distinct()
            .toList();

        LocalDate today = LocalDate.now();
        LocalDate thisMonth = today.withDayOfMonth(1);

        Map<Integer, Long> userDailySecondsMap = studyTimeDailyRepository.findByUserIds(userIds, today).stream()
            .collect(toMap(studyTimeDaily -> studyTimeDaily.getUser().getId(), StudyTimeDaily::getTotalSeconds));
        Map<Integer, Long> userMonthlySecondsMap = studyTimeMonthlyRepository.findByUserIds(userIds, thisMonth).stream()
            .collect(toMap(studyTimeMonthly -> studyTimeMonthly.getUser().getId(), StudyTimeMonthly::getTotalSeconds));

        Map<Integer, Long> clubDailySecondsMap = clubMembers.stream()
            .collect(groupingBy(clubMember -> clubMember.getId().getClubId(),
                summingLong(clubMember -> userDailySecondsMap.getOrDefault(clubMember.getId().getUserId(), 0L))
            ));
        Map<Integer, Long> clubMonthlySecondsMap = clubMembers.stream()
            .collect(groupingBy(clubMember -> clubMember.getId().getClubId(),
                summingLong(clubMember -> userMonthlySecondsMap.getOrDefault(clubMember.getId().getUserId(), 0L))
            ));

        studyTimeRankings.forEach(ranking -> {
            Integer clubId = ranking.getId().getTargetId();
            ranking.updateSeconds(
                clubDailySecondsMap.getOrDefault(clubId, 0L),
                clubMonthlySecondsMap.getOrDefault(clubId, 0L)
            );
        });
    }
}
