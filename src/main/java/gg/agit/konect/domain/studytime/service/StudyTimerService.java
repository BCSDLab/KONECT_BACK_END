package gg.agit.konect.domain.studytime.service;

import static gg.agit.konect.global.code.ApiResponseCode.ALREADY_RUNNING_STUDY_TIMER;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.studytime.dto.StudyTimerStopResponse;
import gg.agit.konect.domain.studytime.model.StudyTimeAggregate;
import gg.agit.konect.domain.studytime.model.StudyTimeDaily;
import gg.agit.konect.domain.studytime.model.StudyTimeMonthly;
import gg.agit.konect.domain.studytime.model.StudyTimeTotal;
import gg.agit.konect.domain.studytime.model.StudyTimer;
import gg.agit.konect.domain.studytime.repository.StudyTimeDailyRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeMonthlyRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeTotalRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimerRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyTimerService {

    private final StudyTimerRepository studyTimerRepository;
    private final StudyTimeDailyRepository studyTimeDailyRepository;
    private final StudyTimeMonthlyRepository studyTimeMonthlyRepository;
    private final StudyTimeTotalRepository studyTimeTotalRepository;
    private final UserRepository userRepository;

    @Transactional
    public void start(Integer userId) {
        if (studyTimerRepository.existsByUserId(userId)) {
            throw CustomException.of(ALREADY_RUNNING_STUDY_TIMER);
        }

        User user = userRepository.getById(userId);
        LocalDateTime startedAt = LocalDateTime.now();

        studyTimerRepository.save(StudyTimer.builder()
            .user(user)
            .startedAt(startedAt)
            .build());
    }

    @Transactional
    public StudyTimerStopResponse stop(Integer userId) {
        StudyTimer studyTimer = studyTimerRepository.getByUserId(userId);

        LocalDateTime endedAt = LocalDateTime.now();
        LocalDateTime startedAt = studyTimer.getStartedAt();
        StudyTimeAggregate aggregate = applyStudyTime(studyTimer.getUser(), startedAt, endedAt);

        studyTimerRepository.delete(studyTimer);

        return StudyTimerStopResponse.of(
            aggregate.sessionSeconds(),
            aggregate.dailySeconds(),
            aggregate.monthlySeconds(),
            aggregate.totalSeconds()
        );
    }

    private StudyTimeAggregate applyStudyTime(User user, LocalDateTime startedAt, LocalDateTime endedAt) {
        LocalDateTime cursor = startedAt;
        long sessionSeconds = 0L;

        while (cursor.toLocalDate().isBefore(endedAt.toLocalDate())) {
            LocalDateTime nextDayStart = cursor.toLocalDate().plusDays(1).atStartOfDay();
            long seconds = Duration.between(cursor, nextDayStart).getSeconds();
            sessionSeconds += addSegment(user, cursor.toLocalDate(), seconds);
            cursor = nextDayStart;
        }

        if (cursor.isBefore(endedAt)) {
            long seconds = Duration.between(cursor, endedAt).getSeconds();
            sessionSeconds += addSegment(user, cursor.toLocalDate(), seconds);
        }

        if (sessionSeconds > 0) {
            addTotalSeconds(user, sessionSeconds);
        }

        LocalDate endDate = endedAt.toLocalDate();

        return buildAggregate(user.getId(), endDate, sessionSeconds);
    }

    private long addSegment(User user, LocalDate date, long seconds) {
        if (seconds <= 0) {
            return 0L;
        }

        StudyTimeDaily daily = studyTimeDailyRepository.findByUserIdAndStudyDate(user.getId(), date)
            .orElseGet(() -> StudyTimeDaily.builder()
                .user(user)
                .studyDate(date)
                .totalSeconds(0L)
                .build());

        daily.addSeconds(seconds);
        studyTimeDailyRepository.save(daily);

        LocalDate month = date.withDayOfMonth(1);
        StudyTimeMonthly monthly = studyTimeMonthlyRepository.findByUserIdAndStudyMonth(user.getId(), month)
            .orElseGet(() -> StudyTimeMonthly.builder()
                .user(user)
                .studyMonth(month)
                .totalSeconds(0L)
                .build());

        monthly.addSeconds(seconds);
        studyTimeMonthlyRepository.save(monthly);

        return seconds;
    }

    private void addTotalSeconds(User user, long seconds) {
        StudyTimeTotal total = studyTimeTotalRepository.findByUserId(user.getId())
            .orElseGet(() -> StudyTimeTotal.builder()
                .user(user)
                .totalSeconds(0L)
                .build());
        total.addSeconds(seconds);
        studyTimeTotalRepository.save(total);
    }

    private StudyTimeAggregate buildAggregate(Integer userId, LocalDate endDate, long sessionSeconds) {
        LocalDate month = endDate.withDayOfMonth(1);

        long dailySeconds = studyTimeDailyRepository.findByUserIdAndStudyDate(userId, endDate)
            .map(StudyTimeDaily::getTotalSeconds)
            .orElse(0L);

        long monthlySeconds = studyTimeMonthlyRepository.findByUserIdAndStudyMonth(userId, month)
            .map(StudyTimeMonthly::getTotalSeconds)
            .orElse(0L);

        long totalSeconds = studyTimeTotalRepository.findByUserId(userId)
            .map(StudyTimeTotal::getTotalSeconds)
            .orElse(0L);

        return new StudyTimeAggregate(sessionSeconds, dailySeconds, monthlySeconds, totalSeconds);
    }
}
