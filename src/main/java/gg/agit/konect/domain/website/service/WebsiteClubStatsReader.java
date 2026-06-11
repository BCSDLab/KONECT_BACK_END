package gg.agit.konect.domain.website.service;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.website.repository.WebsiteQueryRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebsiteClubStatsReader {

    private final WebsiteQueryRepository websiteQueryRepository;
    private final Map<Integer, Long> universityClubCountCache = new ConcurrentHashMap<>();
    private final Map<CategoryCountCacheKey, Map<ClubCategory, Long>> categoryCountCache = new ConcurrentHashMap<>();

    public Long getUniversityClubCount(Integer universityId) {
        return universityClubCountCache.computeIfAbsent(
            universityId,
            websiteQueryRepository::countClubsByUniversityId
        );
    }

    public Map<ClubCategory, Long> getCategoryCounts(Integer universityId, String query) {
        CategoryCountCacheKey key = new CategoryCountCacheKey(universityId, normalizeQuery(query));
        return categoryCountCache.computeIfAbsent(
            key,
            cacheKey -> Map.copyOf(websiteQueryRepository.countClubCategories(
                cacheKey.universityId(),
                cacheKey.query()
            ))
        );
    }

    public void invalidateUniversity(Integer universityId) {
        universityClubCountCache.remove(universityId);
        categoryCountCache.keySet().removeIf(key -> key.universityId().equals(universityId));
    }

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
