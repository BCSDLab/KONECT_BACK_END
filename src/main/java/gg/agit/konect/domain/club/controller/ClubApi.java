package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubsResponse;

import gg.agit.konect.domain.club.dto.JoinedClubsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "(Normal) Club: 동아리", description = "동아리 API")
@RequestMapping("/clubs")
public interface ClubApi {

    @Operation(summary = "페이지 네이션으로 동아리 리스트를 조회한다.", description = """
        - isRecruiting가 true일 경우, 모집일이 빠른 순으로 정렬됩니다.
        """)
    @GetMapping
    ResponseEntity<ClubsResponse> getClubs(
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "10", required = false) Integer limit,
        @RequestParam(name = "query", defaultValue = "", required = false) String query,
        @RequestParam(name = "isRecruiting", defaultValue = "false", required = false) Boolean isRecruiting
    );

    @Operation(summary = "동아리의 상세 정보를 조회한다.", description = """
        - recruitmentStatus는 모집 기간에 따라 BEFORE(모집 전), ONGOING(모집 중), CLOSED(모집 마감)으로 반환됩니다.
        - 모집 일정 데이터가 존재하지 않는다면 CLOSED(모집 마감)으로 간주되며, startDate, endDate는 null로 반환됩니다.
        """)
    @GetMapping("/{clubId}")
    ResponseEntity<ClubDetailResponse> getClubDetail(
        @PathVariable(name = "clubId") Integer clubId
    );

    @Operation(summary = "가입한 동아리 리스트를 조회한다.")
    @GetMapping("/joined")
    ResponseEntity<JoinedClubsResponse> getJoinedClubs();
}
