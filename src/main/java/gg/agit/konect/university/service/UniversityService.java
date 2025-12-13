package gg.agit.konect.university.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.university.dto.UniversitiesResponse;
import gg.agit.konect.university.model.University;
import gg.agit.konect.university.repository.UniversityRepository;

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
