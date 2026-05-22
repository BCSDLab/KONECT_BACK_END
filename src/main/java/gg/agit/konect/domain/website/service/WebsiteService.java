package gg.agit.konect.domain.website.service;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CLUB;
import static gg.agit.konect.global.code.ApiResponseCode.UNIVERSITY_NOT_FOUND;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.university.enums.UniversityRegion;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.website.dto.WebsiteClubDetailResponse;
import gg.agit.konect.domain.website.dto.WebsiteClubListCondition;
import gg.agit.konect.domain.website.dto.WebsiteClubsResponse;
import gg.agit.konect.domain.website.dto.WebsiteHomeResponse;
import gg.agit.konect.domain.website.model.WebsiteUniversitySummary;
import gg.agit.konect.domain.website.repository.WebsiteQueryRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsiteService {

    private final WebsiteQueryRepository websiteQueryRepository;

    public WebsiteHomeResponse getHome(String query, UniversityRegion region) {
        List<WebsiteUniversitySummary> summaries = websiteQueryRepository.findUniversitySummaries(query, region);
        return WebsiteHomeResponse.from(summaries);
    }

    public WebsiteClubsResponse getUniversityClubs(Integer universityId, WebsiteClubListCondition condition) {
        University university = websiteQueryRepository.findUniversity(universityId)
            .orElseThrow(() -> CustomException.of(UNIVERSITY_NOT_FOUND));
        PageRequest pageable = PageRequest.of(condition.page() - 1, condition.limit());

        Page<Club> clubs = websiteQueryRepository.findClubs(
            universityId,
            condition.query(),
            condition.category(),
            pageable
        );
        List<Integer> clubIds = clubs.getContent().stream()
            .map(Club::getId)
            .toList();

        return WebsiteClubsResponse.of(
            university,
            clubs,
            websiteQueryRepository.countMembersByClubIds(clubIds),
            websiteQueryRepository.countClubCategories(universityId, condition.query())
        );
    }

    public WebsiteClubDetailResponse getClubDetail(Integer clubId) {
        Club club = websiteQueryRepository.findClub(clubId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB));
        Long memberCount = websiteQueryRepository.countMembersByClubIds(List.of(clubId)).getOrDefault(clubId, 0L);
        Long universityClubCount = websiteQueryRepository.countClubsByUniversityId(club.getUniversity().getId());

        return WebsiteClubDetailResponse.of(club, memberCount, universityClubCount);
    }

    public WebsiteClubsResponse getRecentClubs(List<Integer> clubIds) {
        List<Integer> distinctClubIds = clubIds.stream()
            .distinct()
            .toList();
        List<Club> clubs = websiteQueryRepository.findClubs(distinctClubIds);
        Map<Integer, Integer> order = createOrder(clubIds);

        List<Club> sortedClubs = clubs.stream()
            .sorted(Comparator.comparingInt(club -> order.getOrDefault(club.getId(), Integer.MAX_VALUE)))
            .toList();

        return WebsiteClubsResponse.recent(
            sortedClubs,
            websiteQueryRepository.countMembersByClubIds(distinctClubIds)
        );
    }

    private Map<Integer, Integer> createOrder(List<Integer> clubIds) {
        Map<Integer, Integer> order = new java.util.LinkedHashMap<>();
        for (int i = 0; i < clubIds.size(); i++) {
            order.putIfAbsent(clubIds.get(i), i);
        }
        return order;
    }
}
