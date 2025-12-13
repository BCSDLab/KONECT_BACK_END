package gg.agit.konect.university.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.university.dto.UniversitiesResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "(Normal) University: 대학", description = "대학 API")
@RequestMapping("/universities")
public interface UniversityApi {

    @Operation(summary = "대학 리스트를 조회한다.")
    @GetMapping
    ResponseEntity<UniversitiesResponse> getUniversities();
}
