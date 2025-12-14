package gg.agit.konect.domain.university.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.university.dto.UniversitiesResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "(Normal) University: 대학", description = "대학 API")
@RequestMapping("/universities")
public interface UniversityApi {

    @Operation(summary = "대학 리스트를 조회한다.", description = """
        - 응답값은 이름 기준 오름차순 정렬됩니다
        """)
    @GetMapping
    ResponseEntity<UniversitiesResponse> getUniversities();
}
