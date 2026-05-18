package gg.agit.konect.unit.domain.studytime.service;

import static gg.agit.konect.global.code.ApiResponseCode.ALREADY_RUNNING_STUDY_TIMER;
import static gg.agit.konect.global.code.ApiResponseCode.STUDY_TIMER_TIME_MISMATCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import gg.agit.konect.domain.studytime.dto.StudyTimerStopRequest;
import gg.agit.konect.domain.studytime.dto.StudyTimerSyncRequest;
import gg.agit.konect.domain.studytime.model.StudyTimer;
import gg.agit.konect.domain.studytime.repository.StudyTimeDailyRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimerRepository;
import gg.agit.konect.domain.studytime.service.StudyTimeQueryService;
import gg.agit.konect.domain.studytime.service.StudyTimerService;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.StudyTimerFixture;
import gg.agit.konect.support.fixture.UserFixture;
import jakarta.persistence.EntityManager;

class StudyTimerServiceTest extends ServiceTestSupport {

    @Mock
    private StudyTimeQueryService studyTimeQueryService;

    @Mock
    private StudyTimerRepository studyTimerRepository;

    @Mock
    private StudyTimeDailyRepository studyTimeDailyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private StudyTimerService studyTimerService;

    @Test
    @DisplayName("이미 실행 중인 타이머가 있으면 새 타이머를 시작하지 않는다")
    void startRejectsAlreadyRunningTimer() {
        // given
        given(studyTimerRepository.existsByUserId(1)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> studyTimerService.start(1))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode())
                .isEqualTo(ALREADY_RUNNING_STUDY_TIMER));

        verifyNoInteractions(userRepository, entityManager, eventPublisher);
        verify(studyTimerRepository, never()).save(any());
    }

    @Test
    @DisplayName("sync 시간 불일치가 3초 이상이면 타이머를 삭제하고 공부 시간을 누적하지 않는다")
    void syncDeletesTimerWhenElapsedTimeMismatches() {
        // given
        StudyTimer studyTimer = createTimerStartedOneHourAgo();
        given(studyTimerRepository.getByUserId(1)).willReturn(studyTimer);

        // when & then
        assertThatThrownBy(() -> studyTimerService.sync(1, new StudyTimerSyncRequest(0L)))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode())
                .isEqualTo(STUDY_TIMER_TIME_MISMATCH));

        verify(studyTimerRepository).delete(studyTimer);
        verifyNoInteractions(studyTimeDailyRepository, studyTimeQueryService);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("stop 시간 불일치가 3초 이상이면 타이머를 삭제하고 요약을 만들지 않는다")
    void stopDeletesTimerWhenElapsedTimeMismatches() {
        // given
        StudyTimer studyTimer = createTimerStartedOneHourAgo();
        given(studyTimerRepository.getByUserId(1)).willReturn(studyTimer);

        // when & then
        assertThatThrownBy(() -> studyTimerService.stop(1, new StudyTimerStopRequest(0L)))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode())
                .isEqualTo(STUDY_TIMER_TIME_MISMATCH));

        verify(studyTimerRepository).delete(studyTimer);
        verifyNoInteractions(studyTimeDailyRepository, studyTimeQueryService);
        verify(eventPublisher, never()).publishEvent(any());
    }

    private StudyTimer createTimerStartedOneHourAgo() {
        User user = UserFixture.createUserWithId(1, "2021136001");
        LocalDateTime startedAt = LocalDateTime.now().minusHours(1);
        return StudyTimerFixture.createStartedTimer(user, startedAt);
    }
}
