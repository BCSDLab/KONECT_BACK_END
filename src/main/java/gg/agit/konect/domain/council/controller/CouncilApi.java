package gg.agit.konect.domain.council.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.council.dto.CouncilCreateRequest;
import gg.agit.konect.domain.council.dto.CouncilResponse;
import gg.agit.konect.domain.council.dto.CouncilUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Council: 총동아리연합회", description = "총동아리연합회 API")
@RequestMapping("/councils")
public interface CouncilApi {

    @Operation(summary = "총동아리연합회 정보를 조회한다.")
    @GetMapping
    ResponseEntity<CouncilResponse> getCouncil();

    @Operation(
        summary = "총동아리연합회 정보를 생성한다.",
        description = """
            총동아리연합회의 기본 정보, 운영 시간, 소셜 미디어 정보를 생성합니다.

            - `INVALID_REQUEST_BODY` (400): 요청 본문의 형식이 올바르지 않거나 필수 값이 누락된 경우
            """
    )
    @PostMapping
    ResponseEntity<Void> createCouncil(
        @Valid @RequestBody CouncilCreateRequest request
    );

    @Operation(
        summary = "총동아리연합회 정보를 수정한다.",
        description = """
            총동아리연합회의 기본 정보, 운영 시간, 소셜 미디어 정보를 수정합니다.

            - `INVALID_REQUEST_BODY` (400): 요청 본문의 형식이 올바르지 않거나 필수 값이 누락된 경우
            - `NOT_FOUND_COUNCIL` (404): 총동아리연합회를 찾을 수 없습니다.
            """
    )
    @PutMapping
    ResponseEntity<Void> updateCouncil(
        @Valid @RequestBody CouncilUpdateRequest request
    );

    @Operation(
        summary = "총동아리연합회 정보를 삭제한다.",
        description = """
            총동아리연합회의 기본 정보와 운영 시간, 소셜 미디어 정보를 모두 삭제합니다.

            - `NOT_FOUND_COUNCIL` (404): 총동아리연합회를 찾을 수 없습니다.
            """
    )
    @DeleteMapping
    ResponseEntity<Void> deleteCouncil();
}
