package gg.agit.konect.domain.university.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gg.agit.konect.domain.university.dto.UniversitiesResponse;

import gg.agit.konect.global.auth.annotation.PublicApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "(Normal) University: 대학교", description = "대학교 API")
@RequestMapping("/universities")
public interface UniversityApi {

    @Operation(summary = "대학교 리스트를 조회한다.", description = """
        - 응답값은 이름 기준 오름차순 정렬됩니다.
        - query 파라미터로 대학교 이름, 초성, 약칭 검색을 지원합니다.
        """)
    @GetMapping
    @PublicApi
    ResponseEntity<UniversitiesResponse> getUniversities(
        @RequestParam(name = "query", required = false) String query
    );
}
