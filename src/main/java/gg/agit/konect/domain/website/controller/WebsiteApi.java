package gg.agit.konect.domain.website.controller;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gg.agit.konect.domain.university.enums.UniversityRegion;
import gg.agit.konect.domain.website.dto.WebsiteClubDetailResponse;
import gg.agit.konect.domain.website.dto.WebsiteClubListCondition;
import gg.agit.konect.domain.website.dto.WebsiteClubsResponse;
import gg.agit.konect.domain.website.dto.WebsiteHomeResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@Validated
@Tag(name = "(Public) Website: 웹사이트 공개 정보")
@RequestMapping("/konect")
public interface WebsiteApi {

    @Operation(summary = "웹사이트 메인 화면 정보를 조회한다.", description = """
        로그인 없이 접근 가능한 웹사이트 메인 정보입니다.
        대학 검색 결과와 대학별 등록 동아리 수를 반환합니다.
        """)
    @ApiResponse(responseCode = "200", content = @Content(
        schema = @Schema(implementation = WebsiteHomeResponse.class),
        examples = @ExampleObject(value = """
            {
              "totalUniversityCount": 1,
              "universities": [
                {
                  "id": 1,
                  "name": "한국기술교육대학교",
                  "campusName": "본교",
                  "region": "CHUNGCHEONG",
                  "regionName": "충청도",
                  "imageUrl": "https://example.com/koreatech-logo.png",
                  "clubCount": 31
                }
              ]
            }
            """)
    ))
    @GetMapping("/home")
    ResponseEntity<WebsiteHomeResponse> getHome(
        @RequestParam(name = "query", required = false) String query,
        @RequestParam(name = "region", required = false) UniversityRegion region
    );

    @Operation(summary = "웹사이트 대학별 동아리 목록을 조회한다.", description = """
        로그인 없이 접근 가능한 대학별 동아리 목록입니다.
        동아리명 검색, 분과 필터, 페이지네이션을 지원합니다.
        """)
    @GetMapping("/universities/{universityId}/clubs")
    ResponseEntity<WebsiteClubsResponse> getUniversityClubs(
        @PathVariable(name = "universityId") Integer universityId,
        @Valid @ParameterObject @ModelAttribute WebsiteClubListCondition condition
    );

    @Operation(summary = "웹사이트 동아리 상세 정보를 조회한다.")
    @GetMapping("/clubs/{clubId}")
    ResponseEntity<WebsiteClubDetailResponse> getClubDetail(
        @PathVariable(name = "clubId") Integer clubId
    );

    @Operation(summary = "최근 본 동아리 카드 정보를 조회한다.", description = """
        프론트엔드가 로컬에 보관한 동아리 ID 목록을 전달하면 카드 표시용 정보를 반환합니다.
        반환 순서는 요청한 clubIds 순서를 따릅니다.
        """)
    @GetMapping("/clubs/recent")
    ResponseEntity<WebsiteClubsResponse> getRecentClubs(
        @RequestParam(name = "clubIds") @Size(min = 1, max = 100) List<Integer> clubIds
    );
}
