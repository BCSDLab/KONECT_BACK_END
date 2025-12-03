package gg.agit.konect.council.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.council.dto.CouncilResponse;
import gg.agit.konect.council.dto.CouncilUpdateRequest;
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
        summary = "총동아리연합회 정보를 수정한다.",
        description = """
            총동아리연합회의 기본 정보, 운영 시간, 소셜 미디어 정보를 수정합니다.

            - `INVALID_OPERATING_HOURS_DAYS` (400): 운영시간은 월요일부터 일요일까지 모두 포함되어야 합니다.
            - `INVALID_OPERATING_HOURS_CLOSED` (400): 휴무일(isClosed=true)에는 시작 시간과 마감 시간이 null이어야 합니다.
            - `INVALID_OPERATING_HOURS_TIME` (400): 운영일(isClosed=false)에는 시작 시간과 마감 시간이 필수이며, 시작 시간이 마감 시간보다 빨라야 합니다.
            - `INVALID_REQUEST_BODY` (400): 요청 본문의 형식이 올바르지 않거나 필수 값이 누락된 경우
            - `NOT_FOUND_COUNCIL` (404): 총동아리연합회를 찾을 수 없습니다.
            """
    )
    @PutMapping
    ResponseEntity<CouncilResponse> updateCouncil(
        @Valid @RequestBody CouncilUpdateRequest request
    );
}
