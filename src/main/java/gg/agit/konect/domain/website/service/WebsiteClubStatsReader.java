package gg.agit.konect.domain.website.service;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.website.repository.WebsiteQueryRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebsiteClubStatsReader {

    private static final long UNIVERSITY_CLUB_COUNT_CACHE_MAX_SIZE = 500;
    private static final long CATEGORY_COUNT_CACHE_MAX_SIZE = 10_000;
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final WebsiteQueryRepository websiteQueryRepository;
    private final Cache<Integer, Long> universityClubCountCache = Caffeine.newBuilder()
        .maximumSize(UNIVERSITY_CLUB_COUNT_CACHE_MAX_SIZE)
        .expireAfterWrite(CACHE_TTL)
        .build();
    private final Cache<CategoryCountCacheKey, Map<ClubCategory, Long>> categoryCountCache = Caffeine.newBuilder()
        .maximumSize(CATEGORY_COUNT_CACHE_MAX_SIZE)
        .expireAfterWrite(CACHE_TTL)
        .build();

    public Long getUniversityClubCount(Integer universityId) {
        return universityClubCountCache.get(
            universityId,
            websiteQueryRepository::countClubsByUniversityId
        );
    }

    public Map<ClubCategory, Long> getCategoryCounts(Integer universityId, String query) {
        CategoryCountCacheKey key = new CategoryCountCacheKey(universityId, normalizeQuery(query));
        return categoryCountCache.get(
            key,
            cacheKey -> Map.copyOf(websiteQueryRepository.countClubCategories(
                cacheKey.universityId(),
                cacheKey.query()
            ))
        );
    }

    public void invalidateUniversity(Integer universityId) {
        universityClubCountCache.invalidate(universityId);
        categoryCountCache.asMap().keySet().removeIf(key -> key.universityId().equals(universityId));
    }

    // null/blank queries share one "no search" cache key; trim and Locale.ROOT keep query keys stable.
    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim().toLowerCase(Locale.ROOT);
    }

    private record CategoryCountCacheKey(
        Integer universityId,
        String query
    ) {
    }
}
