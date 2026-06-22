package gg.agit.konect.unit.domain.website.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.website.repository.WebsiteQueryRepository;
import gg.agit.konect.domain.website.service.WebsiteClubStatsReader;
import gg.agit.konect.support.ServiceTestSupport;

class WebsiteClubStatsReaderTest extends ServiceTestSupport {

    private static final Integer UNIVERSITY_ID = 1;

    @Mock
    private WebsiteQueryRepository websiteQueryRepository;

    private WebsiteClubStatsReader websiteClubStatsReader;

    @BeforeEach
    void setUp() {
        websiteClubStatsReader = new WebsiteClubStatsReader(websiteQueryRepository);
    }

    @Test
    void getUniversityClubCountCachesByUniversity() {
        given(websiteQueryRepository.countClubsByUniversityId(UNIVERSITY_ID)).willReturn(3L);

        Long first = websiteClubStatsReader.getUniversityClubCount(UNIVERSITY_ID);
        Long second = websiteClubStatsReader.getUniversityClubCount(UNIVERSITY_ID);

        assertThat(first).isEqualTo(3L);
        assertThat(second).isEqualTo(3L);
        verify(websiteQueryRepository, times(1)).countClubsByUniversityId(UNIVERSITY_ID);
    }

    @Test
    void getCategoryCountsCachesByUniversityAndNormalizedQuery() {
        given(websiteQueryRepository.countClubCategories(UNIVERSITY_ID, "bcsd"))
            .willReturn(Map.of(ClubCategory.ACADEMIC, 2L));

        Map<ClubCategory, Long> first = websiteClubStatsReader.getCategoryCounts(UNIVERSITY_ID, " bcsd ");
        Map<ClubCategory, Long> second = websiteClubStatsReader.getCategoryCounts(UNIVERSITY_ID, "BCSD");

        assertThat(first).containsEntry(ClubCategory.ACADEMIC, 2L);
        assertThat(second).containsEntry(ClubCategory.ACADEMIC, 2L);
        verify(websiteQueryRepository, times(1)).countClubCategories(UNIVERSITY_ID, "bcsd");
    }

    @Test
    void invalidateUniversityClearsCachedCounts() {
        given(websiteQueryRepository.countClubsByUniversityId(UNIVERSITY_ID)).willReturn(3L, 4L);

        websiteClubStatsReader.getUniversityClubCount(UNIVERSITY_ID);
        websiteClubStatsReader.invalidateUniversity(UNIVERSITY_ID);
        Long refreshedCount = websiteClubStatsReader.getUniversityClubCount(UNIVERSITY_ID);

        assertThat(refreshedCount).isEqualTo(4L);
        verify(websiteQueryRepository, times(2)).countClubsByUniversityId(UNIVERSITY_ID);
    }

    @Test
    void invalidateUniversityIsSafeUnderConcurrentAccess() {
        given(websiteQueryRepository.countClubCategories(eq(UNIVERSITY_ID), anyString()))
            .willReturn(Map.of(ClubCategory.ACADEMIC, 2L));
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < 4; i++) {
                futures.add(executor.submit(() -> {
                    for (int j = 0; j < 100; j++) {
                        websiteClubStatsReader.getCategoryCounts(UNIVERSITY_ID, "query" + j);
                    }
                }));
                futures.add(executor.submit(() -> {
                    for (int j = 0; j < 100; j++) {
                        websiteClubStatsReader.invalidateUniversity(UNIVERSITY_ID);
                    }
                }));
            }

            for (Future<?> future : futures) {
                assertThatCode(() -> future.get(5, TimeUnit.SECONDS)).doesNotThrowAnyException();
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
