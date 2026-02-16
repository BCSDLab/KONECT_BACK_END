package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.club.dto.ClubRecruitmentResponse;
import gg.agit.konect.domain.club.dto.ClubRecruitmentUpsertRequest;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Club - Recruitment: 모집 공고")
@RequestMapping("/clubs")
public interface ClubRecruitmentApi {

    /**
     * Retrieve detailed recruitment information for a club.
     *
     * <p>Response includes the recruitment details, the recruitment period status (`BEFORE`, `ONGOING`, `CLOSED`),
     * and an `isApplied` flag that is `true` when the user is a club member or has an application record.</p>
     *
     * @param clubId the id of the club to query
     * @param userId the id of the requesting user (injected via authentication)
     * @return a response containing the club's recruitment details, status, and `isApplied` flag
     */
    @Operation(summary = "동아리 모집 정보를 조회한다.", description = """
        동아리의 모집 공고 상세 정보를 조회합니다.

        - status는 모집 기간에 따라 BEFORE(모집 전), ONGOING(모집 중), CLOSED(모집 마감)으로 반환됩니다.
        - 동아리 멤버이거나 지원 이력이 존재할 경우 isApplied는 true로 반환됩니다.

        ## 에러
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        - NOT_FOUND_CLUB_RECRUITMENT (404): 동아리 모집 공고를 찾을 수 없습니다.
        """)
    @GetMapping("/{clubId}/recruitments")
    ResponseEntity<ClubRecruitmentResponse> getRecruitments(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    );

    /**
     * Create or update a club recruitment posting.
     *
     * Saves the club recruitment data from the given request: creates a new posting if none exists,
     * or updates the existing posting if one is present.
     *
     * @param request the recruitment details to save
     * @param clubId  the identifier of the target club
     */
    @Operation(summary = "동아리 모집 정보를 생성/수정한다.", description = """
        요청 값을 기준으로 동아리 모집 공고를 저장합니다.
        - 모집 공고가 없으면 생성
        - 모집 공고가 있으면 수정

        ## 에러
        - INVALID_RECRUITMENT_DATE_NOT_ALLOWED (400): 상시 모집인 경우 모집 시작일과 마감일을 지정할 수 없습니다.
        - INVALID_RECRUITMENT_DATE_REQUIRED (400): 상시 모집이 아닐 경우 모집 시작일과 마감일이 필수입니다.
        - INVALID_RECRUITMENT_PERIOD (400): 모집 시작일은 모집 마감일보다 이전이어야 합니다.
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        """)
    @PutMapping("/{clubId}/recruitments")
    ResponseEntity<Void> upsertRecruitment(
        @Valid @RequestBody ClubRecruitmentUpsertRequest request,
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    );

}