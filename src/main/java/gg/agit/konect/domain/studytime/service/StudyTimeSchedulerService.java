package gg.agit.konect.domain.studytime.service;

import static gg.agit.konect.domain.studytime.model.RankingType.*;
import static java.util.stream.Collectors.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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

        Map<Integer, Long> userDailySecondsMap = studyTimeDailies.stream()
            .collect(toMap(
                studyTimeDaily -> studyTimeDaily.getUser().getId(),
                StudyTimeDaily::getTotalSeconds
            ));
        Map<Integer, Long> userMonthlySecondsMap = studyTimeMonthlies.stream()
            .collect(toMap(
                studyTimeMonthly -> studyTimeMonthly.getUser().getId(),
                StudyTimeMonthly::getTotalSeconds
            ));

        List<User> users = Stream.concat(
                studyTimeDailies.stream().map(StudyTimeDaily::getUser),
                studyTimeMonthlies.stream().map(StudyTimeMonthly::getUser)
            )
            .distinct()
            .toList();

        for (User user : users) {
            Integer userId = user.getId();
            University university = user.getUniversity();

            Long dailySeconds = userDailySecondsMap.getOrDefault(userId, 0L);
            Long monthlySeconds = userMonthlySecondsMap.getOrDefault(userId, 0L);

            Optional<StudyTimeRanking> studyTimeRanking = studyTimeRankingRepository.findRanking(
                rankingType.getId(), university.getId(), userId
            );

            if (studyTimeRanking.isEmpty()) {
                studyTimeRankingRepository.save(
                    StudyTimeRanking.of(
                        rankingType,
                        university,
                        userId,
                        user.getName(),
                        dailySeconds,
                        monthlySeconds
                    )
                );
            } else {
                studyTimeRanking.get().updateSeconds(dailySeconds, monthlySeconds);
            }
        }
    }

    @Transactional
    public void updateStudentNumberStudyTimeRanking() {
        LocalDate today = LocalDate.now();
        LocalDate currentMonth = today.withDayOfMonth(1);

        List<StudyTimeDaily> studyTimeDailies = studyTimeDailyRepository.findAllByStudyDate(today);
        List<StudyTimeMonthly> studyTimeMonthlies = studyTimeMonthlyRepository.findAllByStudyMonth(currentMonth);
        if (studyTimeDailies.isEmpty() && studyTimeMonthlies.isEmpty()) {
            return;
        }

        RankingType rankingType = rankingTypeRepository.getByNameIgnoreCase(RANKING_TYPE_STUDENT_NUMBER);

        Map<UniversityYear, Long> universityYearDailyMap = studyTimeDailies.stream()
            .collect(groupingBy(
                studyTimeDaily -> UniversityYear.of(studyTimeDaily.getUser()),
                summingLong(StudyTimeDaily::getTotalSeconds)
            ));
        Map<UniversityYear, Long> universityYearMonthlyMap = studyTimeMonthlies.stream()
            .collect(groupingBy(
                studyTimeMonthly -> UniversityYear.of(studyTimeMonthly.getUser()),
                summingLong(StudyTimeMonthly::getTotalSeconds)
            ));

        Set<UniversityYear> universityYears = new HashSet<>();
        universityYears.addAll(universityYearDailyMap.keySet());
        universityYears.addAll(universityYearMonthlyMap.keySet());

        for (UniversityYear universityYear : universityYears) {
            Long dailySeconds = universityYearDailyMap.getOrDefault(universityYear, 0L);
            Long monthlySeconds = universityYearMonthlyMap.getOrDefault(universityYear, 0L);

            Optional<StudyTimeRanking> studyTimeRanking = studyTimeRankingRepository.findRankingByName(
                rankingType.getId(), universityYear.university().getId(), universityYear.year()
            );

            if (studyTimeRanking.isEmpty()) {
                Integer maxTargetId = studyTimeRankingRepository.findMaxTargetId(
                    rankingType.getId(),
                    universityYear.university().getId()
                );
                Integer nextTargetId = maxTargetId + 1;

                studyTimeRankingRepository.save(
                    StudyTimeRanking.of(
                        rankingType,
                        universityYear.university(),
                        nextTargetId,
                        universityYear.year(),
                        dailySeconds,
                        monthlySeconds
                    )
                );
            } else {
                studyTimeRanking.get().updateSeconds(dailySeconds, monthlySeconds);
            }
        }
    }

    private record UniversityYear(
        University university,
        String year
    ) {
        public static UniversityYear of(User user) {
            return new UniversityYear(
                user.getUniversity(),
                user.getStudentNumber().substring(0, 4)
            );
        }
    }
}
