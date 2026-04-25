package gg.agit.konect.unit.domain.studytime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.studytime.model.RankingType;
import gg.agit.konect.domain.studytime.model.StudyTimeRanking;
import gg.agit.konect.domain.studytime.repository.StudyTimeRankingRepository;
import gg.agit.konect.domain.studytime.service.StudyTimeSchedulerService;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;

class StudyTimeSchedulerServiceTest extends ServiceTestSupport {

    @Mock
    private StudyTimeRankingRepository studyTimeRankingRepository;

    @InjectMocks
    private StudyTimeSchedulerService studyTimeSchedulerService;

    @Test
    @DisplayName("일간 랭킹 초기화는 dailySeconds만 0으로 만들고 monthlySeconds는 유지한다")
    void resetStudyTimeRankingDailyKeepsMonthlySeconds() {
        // given
        StudyTimeRanking ranking = createRanking(120L, 3600L);
        given(studyTimeRankingRepository.findAll()).willReturn(List.of(ranking));

        // when
        studyTimeSchedulerService.resetStudyTimeRankingDaily();

        // then
        assertThat(ranking.getDailySeconds()).isZero();
        assertThat(ranking.getMonthlySeconds()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("월간 랭킹 초기화는 monthlySeconds만 0으로 만들고 dailySeconds는 유지한다")
    void resetStudyTimeRankingMonthlyKeepsDailySeconds() {
        // given
        StudyTimeRanking ranking = createRanking(120L, 3600L);
        given(studyTimeRankingRepository.findAll()).willReturn(List.of(ranking));

        // when
        studyTimeSchedulerService.resetStudyTimeRankingMonthly();

        // then
        assertThat(ranking.getDailySeconds()).isEqualTo(120L);
        assertThat(ranking.getMonthlySeconds()).isZero();
    }

    private StudyTimeRanking createRanking(Long dailySeconds, Long monthlySeconds) {
        RankingType rankingType = new TestRankingType();
        ReflectionTestUtils.setField(rankingType, "id", 1);

        University university = UniversityFixture.createWithId(1);

        return StudyTimeRanking.of(rankingType, university, 1, "BCSD Lab", dailySeconds, monthlySeconds);
    }

    private static class TestRankingType extends RankingType {
    }
}
