package gg.agit.konect.university.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.university.dto.UniversitiesResponse;
import gg.agit.konect.university.service.UniversityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/universities")
public class UniversityController implements UniversityApi {

    private final UniversityService universityService;

    @GetMapping
    public ResponseEntity<UniversitiesResponse> getUniversities() {
        UniversitiesResponse response = universityService.getUniversities();
        return ResponseEntity.ok(response);
    }
}
