package gg.agit.konect.domain.university.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.university.dto.UniversitiesResponse;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.university.repository.UniversityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversityService {

    private final UniversityRepository universityRepository;
    private final UniversitySearchMatcher universitySearchMatcher;
    private final UniversitySearchKeywordReader universitySearchKeywordReader;

    public UniversitiesResponse getUniversities(String query) {
        List<University> universities = universityRepository.findAllByOrderByKoreanNameAsc();
        Map<String, List<String>> keywordsByUniversityName = universitySearchKeywordReader.getKeywordsByUniversityName(
            universities.stream()
                .map(University::getKoreanName)
                .toList()
        );
        List<University> filteredUniversities = universities.stream()
            .filter(university -> universitySearchMatcher.matches(
                university.getKoreanName(),
                query,
                keywordsByUniversityName.getOrDefault(university.getKoreanName(), List.of())
            ))
            .toList();

        return UniversitiesResponse.from(filteredUniversities);
    }
}
