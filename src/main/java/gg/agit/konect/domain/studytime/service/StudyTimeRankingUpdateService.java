package gg.agit.konect.domain.studytime.service;

import static gg.agit.konect.domain.studytime.model.RankingType.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.studytime.model.RankingType;
import gg.agit.konect.domain.studytime.model.StudyTimeRanking;
import gg.agit.konect.domain.studytime.repository.RankingTypeRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeDailyRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeRankingRepository;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyTimeRankingUpdateService {

    private final UserRepository userRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final StudyTimeDailyRepository studyTimeDailyRepository;
    private final StudyTimeRankingRepository studyTimeRankingRepository;
    private final RankingTypeRepository rankingTypeRepository;

    @Transactional
    public void updateRankingsForUser(Integer userId) {
        User user = userRepository.getById(userId);

        updatePersonalRanking(user);
        updateClubRankings(user);
        updateStudentNumberRanking(user);
    }

    private void updatePersonalRanking(User user) {
        RankingType rankingType = rankingTypeRepository.getByNameIgnoreCase(RANKING_TYPE_PERSONAL);
        University university = user.getUniversity();

        Long dailySeconds = getDailySeconds(user.getId());
        Long monthlySeconds = getMonthlySeconds(user.getId());

        Optional<StudyTimeRanking> ranking = studyTimeRankingRepository.findRanking(
            rankingType.getId(), university.getId(), user.getId()
        );

        if (ranking.isEmpty()) {
            studyTimeRankingRepository.save(
                StudyTimeRanking.of(rankingType, university, user.getId(), user.getName(), dailySeconds, monthlySeconds)
            );
        } else {
            ranking.get().updateSeconds(dailySeconds, monthlySeconds);
        }
    }

    private void updateClubRankings(User user) {
        RankingType rankingType = rankingTypeRepository.getByNameIgnoreCase(RANKING_TYPE_CLUB);
        List<ClubMember> clubMembers = clubMemberRepository.findByUserId(user.getId());

        for (ClubMember clubMember : clubMembers) {
            Integer clubId = clubMember.getClub().getId();
            String clubName = clubMember.getClub().getName();
            University university = clubMember.getClub().getUniversity();

            List<Integer> memberUserIds = clubMemberRepository.findUserIdsByClubId(clubId);

            Long dailySeconds = getAggregatedDailySeconds(memberUserIds);
            Long monthlySeconds = getAggregatedMonthlySeconds(memberUserIds);

            Optional<StudyTimeRanking> ranking = studyTimeRankingRepository.findRanking(
                rankingType.getId(), university.getId(), clubId
            );

            if (ranking.isEmpty()) {
                studyTimeRankingRepository.save(
                    StudyTimeRanking.of(rankingType, university, clubId, clubName, dailySeconds, monthlySeconds)
                );
            } else {
                ranking.get().updateSeconds(dailySeconds, monthlySeconds);
            }
        }
    }

    private void updateStudentNumberRanking(User user) {
        RankingType rankingType = rankingTypeRepository.getByNameIgnoreCase(RANKING_TYPE_STUDENT_NUMBER);
        University university = user.getUniversity();
        String studentNumberYear = user.getStudentNumberYear();

        List<Integer> userIds = userRepository.findAllByUniversityIdAndStudentNumberStartingWith(
            university.getId(), studentNumberYear
        ).stream().map(User::getId).toList();

        Long dailySeconds = getAggregatedDailySeconds(userIds);
        Long monthlySeconds = getAggregatedMonthlySeconds(userIds);

        Optional<StudyTimeRanking> ranking = studyTimeRankingRepository.findRankingByName(
            rankingType.getId(), university.getId(), studentNumberYear
        );

        if (ranking.isEmpty()) {
            Integer maxTargetId = studyTimeRankingRepository.findMaxTargetId(
                rankingType.getId(), university.getId()
            );
            Integer nextTargetId = maxTargetId + 1;

            studyTimeRankingRepository.save(
                StudyTimeRanking.of(
                    rankingType, university, nextTargetId,
                    studentNumberYear, dailySeconds, monthlySeconds
                )
            );
        } else {
            ranking.get().updateSeconds(dailySeconds, monthlySeconds);
        }
    }

    private Long getDailySeconds(Integer userId) {
        LocalDate today = LocalDate.now();
        return studyTimeDailyRepository.sumTotalSecondsByUserIdAndStudyDateBetween(userId, today, today);
    }

    private Long getMonthlySeconds(Integer userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        return studyTimeDailyRepository.sumTotalSecondsByUserIdAndStudyDateBetween(userId, monthStart, today);
    }

    private Long getAggregatedDailySeconds(List<Integer> userIds) {
        if (userIds.isEmpty()) {
            return 0L;
        }
        LocalDate today = LocalDate.now();
        return studyTimeDailyRepository.sumTotalSecondsByUserIdsAndStudyDate(userIds, today);
    }

    private Long getAggregatedMonthlySeconds(List<Integer> userIds) {
        if (userIds.isEmpty()) {
            return 0L;
        }
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        return studyTimeDailyRepository.sumTotalSecondsByUserIdsAndStudyDateBetween(userIds, monthStart, today);
    }
}
