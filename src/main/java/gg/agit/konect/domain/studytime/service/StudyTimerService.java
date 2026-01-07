package gg.agit.konect.domain.studytime.service;

import static gg.agit.konect.global.code.ApiResponseCode.ALREADY_RUNNING_STUDY_TIMER;
import static gg.agit.konect.global.code.ApiResponseCode.STUDY_TIMER_TIME_MISMATCH;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.studytime.dto.StudyTimerStopRequest;
import gg.agit.konect.domain.studytime.dto.StudyTimerStopResponse;
import gg.agit.konect.domain.studytime.dto.StudyTimerSyncRequest;
import gg.agit.konect.domain.studytime.model.StudyTimeDaily;
import gg.agit.konect.domain.studytime.model.StudyTimeMonthly;
import gg.agit.konect.domain.studytime.model.StudyTimeSummary;
import gg.agit.konect.domain.studytime.model.StudyTimeTotal;
import gg.agit.konect.domain.studytime.model.StudyTimer;
import gg.agit.konect.domain.studytime.repository.StudyTimeDailyRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeMonthlyRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeTotalRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimerRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyTimerService {

    private static final long TIMER_MISMATCH_THRESHOLD_SECONDS = 3L;

    private final StudyTimeQueryService studyTimeQueryService;
    private final StudyTimerRepository studyTimerRepository;
    private final StudyTimeDailyRepository studyTimeDailyRepository;
    private final StudyTimeMonthlyRepository studyTimeMonthlyRepository;
    private final StudyTimeTotalRepository studyTimeTotalRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    @Transactional
    public void start(Integer userId) {
        if (studyTimerRepository.existsByUserId(userId)) {
            throw CustomException.of(ALREADY_RUNNING_STUDY_TIMER);
        }

        User user = userRepository.getById(userId);
        LocalDateTime startedAt = LocalDateTime.now();

        try {
            studyTimerRepository.save(StudyTimer.of(user, startedAt));
            entityManager.flush();
        } catch (DataIntegrityViolationException e) {
            throw CustomException.of(ALREADY_RUNNING_STUDY_TIMER);
        }
    }

    @Transactional(noRollbackFor = CustomException.class)
    public StudyTimerStopResponse stop(Integer userId, StudyTimerStopRequest request) {
        StudyTimer studyTimer = studyTimerRepository.getByUserId(userId);

        LocalDateTime endedAt = LocalDateTime.now();
        LocalDateTime startedAt = studyTimer.getStartedAt();
        long serverSeconds = Duration.between(startedAt, endedAt).getSeconds();
        long clientSeconds = request.totalSeconds();

        deleteTimerIfElapsedTimeInvalid(studyTimer, serverSeconds, clientSeconds);

        long sessionSeconds = accumulateStudyTime(studyTimer.getUser(), startedAt, endedAt);
        studyTimerRepository.delete(studyTimer);
        StudyTimeSummary summary = buildSummary(userId, sessionSeconds);

        return StudyTimerStopResponse.from(summary);
    }

    @Transactional(noRollbackFor = CustomException.class)
    public void sync(Integer userId, StudyTimerSyncRequest request) {
        StudyTimer studyTimer = studyTimerRepository.getByUserId(userId);

        LocalDateTime syncedAt = LocalDateTime.now();
        LocalDateTime sessionStartedAt = studyTimer.getStartedAt();

        long serverSeconds = Duration.between(sessionStartedAt, syncedAt).getSeconds();
        long clientSeconds = request.totalSeconds();

        deleteTimerIfElapsedTimeInvalid(studyTimer, serverSeconds, clientSeconds);

        accumulateStudyTime(studyTimer.getUser(), studyTimer.getStartedAt(), syncedAt);
        studyTimer.updateStartedAt(syncedAt);
    }

    private long accumulateStudyTime(User user, LocalDateTime startedAt, LocalDateTime endedAt) {
        long sessionSeconds = accumulateDailyAndMonthlySeconds(user, startedAt, endedAt);
        updateTotalSecondsIfNeeded(user, sessionSeconds);

        return sessionSeconds;
    }

    private long accumulateDailyAndMonthlySeconds(User user, LocalDateTime startedAt, LocalDateTime endedAt) {
        LocalDateTime cursor = startedAt;
        long sessionSeconds = 0L;
        LocalDate endDate = endedAt.toLocalDate();

        while (cursor.isBefore(endedAt)) {
            LocalDateTime segmentEnd;

            if (cursor.toLocalDate().isBefore(endDate)) {
                segmentEnd = cursor.toLocalDate().plusDays(1).atStartOfDay();
            } else {
                segmentEnd = endedAt;
            }

            sessionSeconds += accumulateDailyAndMonthlySegment(user, cursor, segmentEnd);
            cursor = segmentEnd;
        }

        return sessionSeconds;
    }

    private long accumulateDailyAndMonthlySegment(User user, LocalDateTime segmentStart, LocalDateTime segmentEnd) {
        if (!segmentStart.isBefore(segmentEnd)) {
            return 0L;
        }

        long seconds = Duration.between(segmentStart, segmentEnd).getSeconds();

        if (seconds <= 0) {
            return 0L;
        }

        LocalDate date = segmentStart.toLocalDate();
        addDailySegment(user, date, seconds);
        addMonthlySegment(user, date, seconds);

        return seconds;
    }

    private void addDailySegment(User user, LocalDate date, long seconds) {
        StudyTimeDaily daily = studyTimeDailyRepository
            .findByUserIdAndStudyDate(user.getId(), date)
            .orElseGet(() -> StudyTimeDaily.of(user, date, 0L));

        daily.addSeconds(seconds);
        studyTimeDailyRepository.save(daily);
    }

    private void addMonthlySegment(User user, LocalDate date, long seconds) {
        LocalDate month = date.withDayOfMonth(1);

        StudyTimeMonthly monthly = studyTimeMonthlyRepository
            .findByUserIdAndStudyMonth(user.getId(), month)
            .orElseGet(() -> StudyTimeMonthly.of(user, month, 0L));

        monthly.addSeconds(seconds);
        studyTimeMonthlyRepository.save(monthly);
    }

    private void updateTotalSecondsIfNeeded(User user, long sessionSeconds) {
        if (sessionSeconds > 0) {
            addTotalSeconds(user, sessionSeconds);
        }
    }

    private void addTotalSeconds(User user, long seconds) {
        StudyTimeTotal total = studyTimeTotalRepository.findByUserId(user.getId())
            .orElseGet(() -> StudyTimeTotal.of(user, 0L));

        total.addSeconds(seconds);
        studyTimeTotalRepository.save(total);
    }

    private StudyTimeSummary buildSummary(Integer userId, long sessionSeconds) {
        long dailySeconds = studyTimeQueryService.getDailyStudyTime(userId);
        long monthlySeconds = studyTimeQueryService.getMonthlyStudyTime(userId);
        long totalSeconds = studyTimeQueryService.getTotalStudyTime(userId);

        return new StudyTimeSummary(sessionSeconds, dailySeconds, monthlySeconds, totalSeconds);
    }

    private void deleteTimerIfElapsedTimeInvalid(StudyTimer studyTimer, long serverSeconds, long clientSeconds) {
        if (Math.abs(serverSeconds - clientSeconds) >= TIMER_MISMATCH_THRESHOLD_SECONDS) {
            studyTimerRepository.delete(studyTimer);
            throw CustomException.of(STUDY_TIMER_TIME_MISMATCH);
        }
    }
}
