package gg.agit.konect.unit.domain.studytime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.studytime.repository.StudyTimeRankingRepository;
import gg.agit.konect.domain.studytime.service.StudyTimeSchedulerService;
import gg.agit.konect.support.ServiceTestSupport;

class StudyTimeSchedulerServiceTest extends ServiceTestSupport {

    @Mock
    private StudyTimeRankingRepository studyTimeRankingRepository;

    @InjectMocks
    private StudyTimeSchedulerService studyTimeSchedulerService;

    @Test
    @DisplayName("월초에는 일간과 월간 랭킹을 한 번에 초기화한다")
    void resetStudyTimeRankingResetsDailyAndMonthlyOnFirstDayOfMonth() {
        // given
        LocalDate firstDayOfMonth = LocalDate.of(2026, 5, 1);
        when(studyTimeRankingRepository.resetDailyAndMonthlySeconds()).thenReturn(10);

        // when
        int updatedCount = studyTimeSchedulerService.resetStudyTimeRanking(firstDayOfMonth);

        // then
        assertThat(updatedCount).isEqualTo(10);
        verify(studyTimeRankingRepository).resetDailyAndMonthlySeconds();
        verify(studyTimeRankingRepository, never()).resetDailySeconds();
    }

    @Test
    @DisplayName("월초가 아니면 일간 랭킹만 초기화한다")
    void resetStudyTimeRankingResetsOnlyDailyOnNonFirstDayOfMonth() {
        // given
        LocalDate nonFirstDayOfMonth = LocalDate.of(2026, 5, 2);
        when(studyTimeRankingRepository.resetDailySeconds()).thenReturn(7);

        // when
        int updatedCount = studyTimeSchedulerService.resetStudyTimeRanking(nonFirstDayOfMonth);

        // then
        assertThat(updatedCount).isEqualTo(7);
        verify(studyTimeRankingRepository).resetDailySeconds();
        verify(studyTimeRankingRepository, never()).resetDailyAndMonthlySeconds();
    }
}
