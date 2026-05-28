package gg.agit.konect.domain.university.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.university.dto.UniversitiesResponse;
import gg.agit.konect.domain.university.service.UniversityService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/universities")
public class UniversityController implements UniversityApi {

    private final UniversityService universityService;

    @Override
    public ResponseEntity<UniversitiesResponse> getUniversities(String query) {
        UniversitiesResponse response = universityService.getUniversities(query);
        return ResponseEntity.ok(response);
    }
}
