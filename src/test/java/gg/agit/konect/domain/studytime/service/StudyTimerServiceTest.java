package gg.agit.konect.domain.studytime.service;

import static gg.agit.konect.global.code.ApiResponseCode.ALREADY_RUNNING_STUDY_TIMER;
import static gg.agit.konect.global.code.ApiResponseCode.STUDY_TIMER_TIME_MISMATCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.dao.DataIntegrityViolationException;

import gg.agit.konect.domain.studytime.dto.StudyTimerStopRequest;
import gg.agit.konect.domain.studytime.dto.StudyTimerStopResponse;
import gg.agit.konect.domain.studytime.dto.StudyTimerSyncRequest;
import gg.agit.konect.domain.studytime.model.StudyTimeDaily;
import gg.agit.konect.domain.studytime.model.StudyTimeMonthly;
import gg.agit.konect.domain.studytime.model.StudyTimeTotal;
import gg.agit.konect.domain.studytime.model.StudyTimer;
import gg.agit.konect.domain.studytime.repository.StudyTimeDailyRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeMonthlyRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeTotalRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimerRepository;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.global.model.BaseEntity;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;
import jakarta.persistence.EntityManager;

class StudyTimerServiceTest extends ServiceTestSupport {

    @Mock
    private StudyTimeQueryService studyTimeQueryService;

    @Mock
    private StudyTimerRepository studyTimerRepository;

    @Mock
    private StudyTimeDailyRepository studyTimeDailyRepository;

    @Mock
    private StudyTimeMonthlyRepository studyTimeMonthlyRepository;

    @Mock
    private StudyTimeTotalRepository studyTimeTotalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private StudyTimerService studyTimerService;

    @Test
    @DisplayName("start는 새 타이머를 저장하고 flush 한다")
    void startSavesNewTimerAndFlushes() {
        // given
        Integer userId = 1;
        User user = createUser(userId);
        when(studyTimerRepository.existsByUserId(userId)).thenReturn(false);
        when(userRepository.getById(userId)).thenReturn(user);

        ArgumentCaptor<StudyTimer> timerCaptor = ArgumentCaptor.forClass(StudyTimer.class);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        // when
        studyTimerService.start(userId);

        // then
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        verify(studyTimerRepository).save(timerCaptor.capture());
        verify(entityManager).flush();

        StudyTimer savedTimer = timerCaptor.getValue();
        assertThat(savedTimer.getUser()).isSameAs(user);
        assertThat(savedTimer.getStartedAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("start는 저장 경합으로 flush가 실패하면 이미 실행 중 오류로 변환한다")
    void startThrowsAlreadyRunningWhenFlushFailsWithConstraintViolation() {
        // given
        Integer userId = 1;
        User user = createUser(userId);
        when(studyTimerRepository.existsByUserId(userId)).thenReturn(false);
        when(userRepository.getById(userId)).thenReturn(user);
        doThrow(new DataIntegrityViolationException("duplicate timer")).when(entityManager).flush();

        // when & then
        assertErrorCode(
            () -> studyTimerService.start(userId),
            ALREADY_RUNNING_STUDY_TIMER
        );
    }

    @Test
    @DisplayName("stop는 클라이언트 시간 차이가 임계값 이상이면 타이머를 삭제하고 실패한다")
    void stopDeletesTimerWhenElapsedTimeMismatchReachesThreshold() throws Exception {
        // given
        Integer userId = 1;
        LocalDateTime fixedNow = LocalDateTime.of(2026, 5, 1, 0, 0, 1);
        StudyTimer studyTimer = timer(createUser(userId), fixedNow.minusSeconds(3), fixedNow.minusSeconds(3));
        when(studyTimerRepository.getByUserId(userId)).thenReturn(studyTimer);

        // when & then
        withFixedNow(fixedNow, () -> assertErrorCode(
            () -> studyTimerService.stop(userId, new StudyTimerStopRequest(0L)),
            STUDY_TIMER_TIME_MISMATCH
        ));

        verify(studyTimerRepository).delete(studyTimer);
        verify(studyTimeDailyRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(studyTimeMonthlyRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(studyTimeTotalRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("sync는 날짜와 월 경계를 넘는 누적 시간을 각각 분할 저장한다")
    void syncSplitsAccumulationAcrossDateAndMonthBoundaries() throws Exception {
        // given
        Integer userId = 1;
        User user = createUser(userId);
        LocalDateTime fixedNow = LocalDateTime.of(2026, 5, 1, 0, 0, 1);
        StudyTimer studyTimer = timer(user, fixedNow.minusSeconds(3), fixedNow.minusSeconds(3));
        when(studyTimerRepository.getByUserId(userId)).thenReturn(studyTimer);
        when(studyTimeDailyRepository.findByUserIdAndStudyDate(userId, LocalDate.of(2026, 4, 30)))
            .thenReturn(Optional.empty());
        when(studyTimeDailyRepository.findByUserIdAndStudyDate(userId, LocalDate.of(2026, 5, 1)))
            .thenReturn(Optional.empty());
        when(studyTimeMonthlyRepository.findByUserIdAndStudyMonth(userId, LocalDate.of(2026, 4, 1)))
            .thenReturn(Optional.empty());
        when(studyTimeMonthlyRepository.findByUserIdAndStudyMonth(userId, LocalDate.of(2026, 5, 1)))
            .thenReturn(Optional.empty());
        when(studyTimeTotalRepository.findByUserId(userId)).thenReturn(Optional.empty());

        ArgumentCaptor<StudyTimeDaily> dailyCaptor = ArgumentCaptor.forClass(StudyTimeDaily.class);
        ArgumentCaptor<StudyTimeMonthly> monthlyCaptor = ArgumentCaptor.forClass(StudyTimeMonthly.class);
        ArgumentCaptor<StudyTimeTotal> totalCaptor = ArgumentCaptor.forClass(StudyTimeTotal.class);

        // when
        withFixedNow(fixedNow, () -> studyTimerService.sync(userId, new StudyTimerSyncRequest(3L)));

        // then
        verify(studyTimeDailyRepository, org.mockito.Mockito.times(2)).save(dailyCaptor.capture());
        verify(studyTimeMonthlyRepository, org.mockito.Mockito.times(2)).save(monthlyCaptor.capture());
        verify(studyTimeTotalRepository).save(totalCaptor.capture());

        assertThat(dailyCaptor.getAllValues())
            .extracting(StudyTimeDaily::getStudyDate, StudyTimeDaily::getTotalSeconds)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 4, 30), 2L),
                org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 5, 1), 1L)
            );
        assertThat(monthlyCaptor.getAllValues())
            .extracting(StudyTimeMonthly::getStudyMonth, StudyTimeMonthly::getTotalSeconds)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 4, 1), 2L),
                org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 5, 1), 1L)
            );
        assertThat(totalCaptor.getValue().getTotalSeconds()).isEqualTo(3L);
        assertThat(studyTimer.getStartedAt()).isEqualTo(fixedNow);
    }

    @Test
    @DisplayName("stop는 누적 저장 후 최신 집계값으로 응답을 구성한다")
    void stopReturnsSummaryAfterPersistingElapsedTime() throws Exception {
        // given
        Integer userId = 1;
        User user = createUser(userId);
        LocalDateTime fixedNow = LocalDateTime.of(2026, 4, 10, 12, 0, 2);
        StudyTimer studyTimer = timer(user, fixedNow.minusSeconds(2), fixedNow.minusSeconds(2));
        when(studyTimerRepository.getByUserId(userId)).thenReturn(studyTimer);
        when(studyTimeDailyRepository.findByUserIdAndStudyDate(userId, fixedNow.toLocalDate()))
            .thenReturn(Optional.empty());
        when(studyTimeMonthlyRepository.findByUserIdAndStudyMonth(userId, fixedNow.toLocalDate().withDayOfMonth(1)))
            .thenReturn(Optional.empty());
        when(studyTimeTotalRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(studyTimeQueryService.getDailyStudyTime(userId)).thenReturn(120L);
        when(studyTimeQueryService.getMonthlyStudyTime(userId)).thenReturn(860L);
        when(studyTimeQueryService.getTotalStudyTime(userId)).thenReturn(5400L);

        // when
        StudyTimerStopResponse response = withFixedNow(
            fixedNow,
            () -> studyTimerService.stop(userId, new StudyTimerStopRequest(2L))
        );

        // then
        assertThat(response.sessionSeconds()).isEqualTo(2L);
        assertThat(response.dailySeconds()).isEqualTo(120L);
        assertThat(response.monthlySeconds()).isEqualTo(860L);
        assertThat(response.totalSeconds()).isEqualTo(5400L);
        verify(studyTimerRepository).delete(studyTimer);
        verify(studyTimeTotalRepository).save(org.mockito.ArgumentMatchers.any(StudyTimeTotal.class));
    }

    private User createUser(Integer userId) {
        return User.builder()
            .id(userId)
            .university(UniversityFixture.create())
            .email("user" + userId + "@koreatech.ac.kr")
            .name("테스트유저" + userId)
            .studentNumber("2021000" + userId)
            .role(UserRole.USER)
            .isMarketingAgreement(true)
            .imageUrl("https://example.com/profile.png")
            .build();
    }

    private StudyTimer timer(User user, LocalDateTime createdAt, LocalDateTime startedAt) throws Exception {
        StudyTimer timer = StudyTimer.of(user, startedAt);
        setBaseEntityField(timer, "createdAt", createdAt);
        setBaseEntityField(timer, "updatedAt", createdAt);
        return timer;
    }

    private void setBaseEntityField(Object target, String fieldName, Object value) throws Exception {
        Field field = BaseEntity.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void assertErrorCode(ThrowingCallable callable, gg.agit.konect.global.code.ApiResponseCode errorCode) {
        assertThatThrownBy(callable::call)
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode()).isEqualTo(errorCode));
    }

    private void withFixedNow(LocalDateTime fixedNow, ThrowingRunnable runnable) throws Exception {
        try (MockedStatic<LocalDateTime> mocked = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            mocked.when(LocalDateTime::now).thenReturn(fixedNow);
            runnable.run();
        }
    }

    private <T> T withFixedNow(LocalDateTime fixedNow, ThrowingSupplier<T> supplier) throws Exception {
        try (MockedStatic<LocalDateTime> mocked = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            mocked.when(LocalDateTime::now).thenReturn(fixedNow);
            return supplier.get();
        }
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
