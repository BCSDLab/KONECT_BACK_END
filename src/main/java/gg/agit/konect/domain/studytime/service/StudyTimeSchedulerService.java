package gg.agit.konect.domain.studytime.service;

import static gg.agit.konect.domain.studytime.model.RankingType.*;
import static java.util.stream.Collectors.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyTimeSchedulerService {

    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;
    private final StudyTimeDailyRepository studyTimeDailyRepository;
    private final StudyTimeMonthlyRepository studyTimeMonthlyRepository;
    private final StudyTimeRankingRepository studyTimeRankingRepository;
    private final RankingTypeRepository rankingTypeRepository;

    @Transactional
    public void updateClubStudyTimeRanking() {
        RankingType rankingType = rankingTypeRepository.getByNameIgnoreCase(RANKING_TYPE_CLUB);
        List<StudyTimeRanking> studyTimeRankings = studyTimeRankingRepository.findByRankingTypeId(rankingType.getId());
        if (studyTimeRankings.isEmpty()) {
            return;
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

    @Transactional
    public void updatePersonalStudyTimeRanking() {
        LocalDate today = LocalDate.now();
        LocalDate currentMonth = today.withDayOfMonth(1);

        List<StudyTimeDaily> studyTimeDailies = studyTimeDailyRepository.findAllByStudyDate(today);
        List<StudyTimeMonthly> studyTimeMonthlies = studyTimeMonthlyRepository.findAllByStudyMonth(currentMonth);
        if (studyTimeDailies.isEmpty() && studyTimeMonthlies.isEmpty()) {
            return;
        }

        RankingType rankingType = rankingTypeRepository.getByNameIgnoreCase(RANKING_TYPE_PERSONAL);

        for (StudyTimeDaily studyTimeDaily : studyTimeDailies) {
            User user = studyTimeDaily.getUser();
            University university = user.getUniversity();

            Optional<StudyTimeRanking> studyTimeRanking = studyTimeRankingRepository.findRanking(
                rankingType.getId(), university.getId(), user.getId()
            );

            if (studyTimeRanking.isEmpty()) {
                studyTimeRankingRepository.save(
                    StudyTimeRanking.of(
                        rankingType,
                        university,
                        user.getId(),
                        user.getName(),
                        studyTimeDaily.getTotalSeconds(),
                        null
                    )
                );
            } else {
                studyTimeRanking.get().updateDailySeconds(studyTimeDaily.getTotalSeconds());
            }
        }

        for (StudyTimeMonthly studyTimeMonthly : studyTimeMonthlies) {
            User user = studyTimeMonthly.getUser();
            University university = user.getUniversity();

            Optional<StudyTimeRanking> studyTimeRanking = studyTimeRankingRepository.findRanking(
                rankingType.getId(), university.getId(), user.getId()
            );

            if (studyTimeRanking.isEmpty()) {
                studyTimeRankingRepository.save(
                    StudyTimeRanking.of(
                        rankingType,
                        university,
                        user.getId(),
                        user.getName(),
                        null,
                        studyTimeMonthly.getTotalSeconds()
                    )
                );
            } else {
                studyTimeRanking.get().updateMonthlySeconds(studyTimeMonthly.getTotalSeconds());
            }
        }
    }

    @Transactional
    public void updateStudentNumberStudyTimeRanking() {
        record UniversityYear(Integer universityId, String year) {
        }

        RankingType rankingType = rankingTypeRepository.getByNameIgnoreCase(RANKING_TYPE_STUDENT_NUMBER);
        List<StudyTimeRanking> studyTimeRankings = studyTimeRankingRepository.findByRankingTypeId(rankingType.getId());

        if (studyTimeRankings.isEmpty()) {
            return;
        }

        List<UniversityYear> universityYears = studyTimeRankings.stream()
            .map(ranking -> new UniversityYear(
                ranking.getId().getUniversityId(),
                ranking.getTargetName()
            ))
            .distinct()
            .toList();

        LocalDate today = LocalDate.now();
        LocalDate thisMonth = today.withDayOfMonth(1);

        Map<UniversityYear, List<Integer>> universityYearToUserIdsMap = universityYears.stream()
            .collect(toMap(
                universityYear -> universityYear,
                universityYear -> userRepository.findUserIdsByUniversityAndStudentYear(
                        universityYear.universityId(),
                        universityYear.year()
                    ).stream()
                    .map(User::getId)
                    .toList()
            ));

        List<Integer> userIds = universityYearToUserIdsMap.values().stream()
            .flatMap(List::stream)
            .distinct()
            .toList();

        Map<Integer, Long> userDailySecondsMap = studyTimeDailyRepository.findByUserIds(userIds, today).stream()
            .collect(toMap(studyTimeDaily -> studyTimeDaily.getUser().getId(), StudyTimeDaily::getTotalSeconds,
                (existing, replacement) -> existing));
        Map<Integer, Long> userMonthlySecondsMap = studyTimeMonthlyRepository.findByUserIds(userIds, thisMonth)
            .stream()
            .collect(toMap(studyTimeMonthly -> studyTimeMonthly.getUser().getId(), StudyTimeMonthly::getTotalSeconds,
                (existing, replacement) -> existing));

        Map<UniversityYear, Long> universityYearDailySecondsMap = universityYearToUserIdsMap.entrySet().stream()
            .collect(toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .mapToLong(userId -> userDailySecondsMap.getOrDefault(userId, 0L))
                    .sum()
            ));
        Map<UniversityYear, Long> universityYearMonthlySecondsMap = universityYearToUserIdsMap.entrySet().stream()
            .collect(toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .mapToLong(userId -> userMonthlySecondsMap.getOrDefault(userId, 0L))
                    .sum()
            ));

        studyTimeRankings.forEach(ranking -> {
            UniversityYear key = new UniversityYear(
                ranking.getId().getUniversityId(),
                ranking.getTargetName()
            );
            ranking.updateSeconds(
                universityYearDailySecondsMap.getOrDefault(key, 0L),
                universityYearMonthlySecondsMap.getOrDefault(key, 0L)
            );
        });
    }
}
