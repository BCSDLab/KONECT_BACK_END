package gg.agit.konect.domain.club.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.club.dto.ClubApplyQuestionsReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyRequest;
import gg.agit.konect.domain.club.dto.ClubCondition;
import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubFeeInfoReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubFeeInfoResponse;
import gg.agit.konect.domain.club.dto.ClubMembersResponse;
import gg.agit.konect.domain.club.dto.ClubMembershipsResponse;
import gg.agit.konect.domain.club.dto.ClubRecruitmentCreateRequest;
import gg.agit.konect.domain.club.dto.ClubRecruitmentResponse;
import gg.agit.konect.domain.club.dto.ClubRecruitmentUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubsResponse;
import gg.agit.konect.global.auth.annotation.ClubManagerOnly;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Club: 동아리", description = "동아리 API")
@RequestMapping("/clubs")
public interface ClubApi {

    @Operation(summary = "페이지 네이션으로 동아리 리스트를 조회한다.", description = """
        - isRecruiting가 true일 경우, 모집일이 빠른 순으로 정렬됩니다.
        - status은 BEFORE(모집 전), ONGOING(모집 중), CLOSED(모집 마감)으로 반환됩니다.
        """)
    @GetMapping
    ResponseEntity<ClubsResponse> getClubs(
        @Valid @ParameterObject @ModelAttribute ClubCondition condition,
        @UserId Integer userId
    );

    @Operation(summary = "동아리의 상세 정보를 조회한다.", description = """
        - recruitmentStatus는 모집 기간에 따라 BEFORE(모집 전), ONGOING(모집 중), CLOSED(모집 마감)으로 반환됩니다.
        - 모집 일정 데이터가 존재하지 않는다면 CLOSED(모집 마감)으로 간주되며, startDate, endDate는 null로 반환됩니다.
        - 동아리 멤버이거나 지원 이력이 존재할 경우 isApplied는 true로 반환됩니다.
        """)
    @GetMapping("/{clubId}")
    ResponseEntity<ClubDetailResponse> getClubDetail(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    );

    @Operation(summary = "가입한 동아리 리스트를 조회한다.")
    @GetMapping("/joined")
    ResponseEntity<ClubMembershipsResponse> getJoinedClubs(
        @UserId Integer userId
    );

    @Operation(summary = "관리자 권한을 가지고 있는 동아리 리스트를 조회한다.")
    @GetMapping("/managed")
    ResponseEntity<ClubMembershipsResponse> getManagedClubs(
        @UserId Integer userId
    );

    @Operation(summary = "동아리 멤버 리스트를 조회한다.")
    @GetMapping("/{clubId}/members")
    ResponseEntity<ClubMembersResponse> getClubMembers(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 가입 신청을 한다.", description = """
        동아리 가입 신청서를 제출합니다.
        설문 질문이 없는 경우 answers는 빈 배열을 전달합니다.
        
        - ALREADY_APPLIED_CLUB (409): 이미 가입 신청을 완료한 사용자입니다.
        - NOT_FOUND_CLUB_APPLY_QUESTION (404): 존재하지 않는 가입 문항입니다.
        - DUPLICATE_CLUB_APPLY_QUESTION (409): 중복된 id의 가입 문항이 포함되어 있습니다.
        - REQUIRED_CLUB_APPLY_ANSWER_MISSING (400): 필수 가입 답변이 누락되었습니다.
        """)
    @PostMapping("/{clubId}/apply")
    ResponseEntity<ClubFeeInfoResponse> applyClub(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubApplyRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 회비 정보를 조회한다.", description = """
        동아리 가입 신청을 완료했거나 동아리 관리자 권한이 있는 사용자만 회비 계좌 정보를 조회할 수 있습니다.

        ## 에러
        - FORBIDDEN_CLUB_FEE_INFO (403): 회비 정보 조회 권한이 없습니다.
        """)
    @GetMapping("/{clubId}/fee")
    ResponseEntity<ClubFeeInfoResponse> getFeeInfo(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    );

    @ClubManagerOnly
    @Operation(summary = "동아리 회비 정보를 덮어써서 대체한다.", description = """
        요청 본문이 최종 상태가 됩니다.
        - 모든 필드를 전달하면 생성/수정합니다.
        - 모든 필드가 null이면 회비 정보를 삭제합니다.
        - 일부 필드가 누락된 경우 에러가 발생합니다.

        ## 에러
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - INVALID_REQUEST_BODY (400): 요청 본문의 형식이 올바르지 않거나 필수 값이 누락된 경우
        """)
    @PutMapping("/{clubId}/fee")
    ResponseEntity<ClubFeeInfoResponse> replaceFeeInfo(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubFeeInfoReplaceRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 가입 문항을 조회한다.")
    @GetMapping("/{clubId}/questions")
    ResponseEntity<ClubApplyQuestionsResponse> getApplyQuestions(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    );

    @ClubManagerOnly
    @Operation(summary = "동아리 가입 문항을 덮어써서 대체한다.", description = """
        요청에 포함된 문항 목록이 최종 상태가 됩니다.
        - questionId가 있으면 수정
        - questionId가 없으면 생성
        - 요청에 없는 기존 문항은 삭제됩니다.
        - 저장된 문항 목록을 반환합니다.
        
        ## 에러
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - NOT_FOUND_CLUB_APPLY_QUESTION (404): 존재하지 않는 가입 문항입니다.
        - DUPLICATE_CLUB_APPLY_QUESTION (409): 중복된 id의 가입 문항이 포함되어 있습니다.
        """)
    @PutMapping("/{clubId}/questions")
    ResponseEntity<ClubApplyQuestionsResponse> replaceApplyQuestions(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubApplyQuestionsReplaceRequest request,
        @UserId Integer userId
    );

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

    @Operation(summary = "동아리 모집 정보를 생성한다.", description = """
        동아리 회장만 모집 공고를 생성할 수 있습니다.
        한 동아리당 하나의 모집 공고만 생성 가능합니다.
        
        ## 에러
        - INVALID_RECRUITMENT_DATE_NOT_ALLOWED (400): 상시 모집인 경우 모집 시작일과 마감일을 지정할 수 없습니다.
        - INVALID_RECRUITMENT_DATE_REQUIRED (400): 상시 모집이 아닐 경우 모집 시작일과 마감일이 필수입니다.
        - INVALID_RECRUITMENT_PERIOD (400): 모집 시작일은 모집 마감일보다 이전이어야 합니다.
        - FORBIDDEN_CLUB_RECRUITMENT_CREATE (403): 동아리 모집 공고를 생성할 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        - ALREADY_EXIST_CLUB_RECRUITMENT (409): 이미 동아리 모집 공고가 존재합니다.
        """)
    @PostMapping("/{clubId}/recruitments")
    ResponseEntity<Void> createRecruitment(
        @RequestBody @Valid ClubRecruitmentCreateRequest request,
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    );

    @ClubManagerOnly
    @Operation(summary = "동아리 모집 정보를 수정한다.", description = """
        동아리 회장 또는 매니저만 모집 공고를 수정할 수 있습니다.

        ## 에러
        - INVALID_RECRUITMENT_DATE_NOT_ALLOWED (400): 상시 모집인 경우 모집 시작일과 마감일을 지정할 수 없습니다.
        - INVALID_RECRUITMENT_DATE_REQUIRED (400): 상시 모집이 아닐 경우 모집 시작일과 마감일이 필수입니다.
        - INVALID_RECRUITMENT_PERIOD (400): 모집 시작일은 모집 마감일보다 이전이어야 합니다.
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        - NOT_FOUND_CLUB_RECRUITMENT (404): 동아리 모집 공고를 찾을 수 없습니다.
        """)
    @PutMapping("/{clubId}/recruitments")
    ResponseEntity<Void> updateRecruitment(
        @Valid @RequestBody ClubRecruitmentUpdateRequest request,
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    );
}
