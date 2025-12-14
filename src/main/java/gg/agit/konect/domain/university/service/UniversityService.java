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

    public UniversitiesResponse getUniversities() {
        List<University> universities = universityRepository.findAllByOrderByKoreanNameAsc();
        return UniversitiesResponse.from(universities);
    }
}
