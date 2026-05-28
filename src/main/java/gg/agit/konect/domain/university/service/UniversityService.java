package gg.agit.konect.domain.university.service;

import java.util.List;

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

    public UniversitiesResponse getUniversities(String query) {
        List<University> universities = universityRepository.findAllByOrderByKoreanNameAsc();
        List<University> filteredUniversities = universities.stream()
            .filter(university -> universitySearchMatcher.matches(university, query))
            .toList();

        return UniversitiesResponse.from(filteredUniversities);
    }
}
