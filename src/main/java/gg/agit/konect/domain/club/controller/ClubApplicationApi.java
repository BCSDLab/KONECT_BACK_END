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

import gg.agit.konect.domain.club.dto.ClubApplicationAnswersResponse;
import gg.agit.konect.domain.club.dto.ClubApplicationCondition;
import gg.agit.konect.domain.club.dto.ClubApplicationsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyRequest;
import gg.agit.konect.domain.club.dto.ClubFeeInfoReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubFeeInfoResponse;
import gg.agit.konect.global.auth.annotation.PublicApi;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Club - Application: 지원 및 신청")
@RequestMapping("/clubs")
public interface ClubApplicationApi {

    @Operation(summary = "동아리 가입 신청을 한다.", description = """
        동아리 가입 신청서를 제출합니다.
        설문 질문이 없는 경우 answers는 빈 배열을 전달합니다.
        모집 공고에서 회비 납부가 필요한 경우(isFeeRequired=true), feePaymentImageUrl은 필수입니다.

        ## 에러
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        - ALREADY_APPLIED_CLUB (409): 이미 가입 신청을 완료한 사용자입니다.
        - NOT_FOUND_CLUB_APPLY_QUESTION (404): 존재하지 않는 가입 문항입니다.
        - DUPLICATE_CLUB_APPLY_QUESTION (409): 중복된 id의 가입 문항이 포함되어 있습니다.
        - REQUIRED_CLUB_APPLY_ANSWER_MISSING (400): 필수 가입 답변이 누락되었습니다.
        - FEE_PAYMENT_IMAGE_REQUIRED (400): 회비 납부가 필요한 동아리입니다. 납부 증빙 사진을 첨부해주세요.
        """)
    @PostMapping("/{clubId}/apply")
    ResponseEntity<ClubFeeInfoResponse> applyClub(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubApplyRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 지원 내역을 조회한다.", description = """
         - 동아리 관리자만 해당 동아리의 지원 내역을 조회할 수 있습니다.
         - 현재 지정된 모집 일정 범위에 지원한 내역만 볼 수 있습니다.
         - 상시 모집의 경우 모든 내역을 봅니다.
         - 정렬 기준: APPLIED_AT(신청 일시), STUDENT_NUMBER(학번), NAME(이름)
         - 정렬 방향: ASC(오름차순), DESC(내림차순)
         - 기본 정렬: 신청 일시 최신순 (APPLIED_AT DESC)
        
        ## 에러
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_CLUB_RECRUITMENT (404): 동아리 모집 공고를 찾을 수 없습니다.
        """)
    @GetMapping("/{clubId}/applications")
    ResponseEntity<ClubApplicationsResponse> getClubApplications(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @ParameterObject @ModelAttribute ClubApplicationCondition condition,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 지원 답변을 조회한다.", description = """
        - 동아리 관리자만 해당 동아리의 지원 답변을 조회할 수 있습니다.
        
        ## 에러
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_CLUB_APPLY (404): 동아리 지원 내역을 찾을 수 없습니다.
        """)
    @GetMapping("/{clubId}/applications/{applicationId}")
    ResponseEntity<ClubApplicationAnswersResponse> getClubApplicationAnswers(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "applicationId") Integer applicationId,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 가입 신청을 승인한다.", description = """
        동아리 회장 또는 부회장만 가입 신청을 승인할 수 있습니다.
        승인 시 지원자는 일반회원으로 등록되며, 지원 내역은 삭제됩니다.
        
        ## 에러
        - ALREADY_CLUB_MEMBER (409): 이미 동아리 회원입니다.
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_CLUB_APPLY (404): 동아리 지원 내역을 찾을 수 없습니다.
        """)
    @PostMapping("/{clubId}/applications/{applicationId}/approve")
    ResponseEntity<Void> approveClubApplication(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "applicationId") Integer applicationId,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 가입 신청을 거절한다.", description = """
        동아리 회장 또는 부회장만 가입 신청을 거절할 수 있습니다.
        거절 시 지원 내역은 삭제됩니다.
        
        ## 에러
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_CLUB_APPLY (404): 동아리 지원 내역을 찾을 수 없습니다.
        """)
    @PostMapping("/{clubId}/applications/{applicationId}/reject")
    ResponseEntity<Void> rejectClubApplication(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "applicationId") Integer applicationId,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 가입 문항을 조회한다.")
    @GetMapping("/{clubId}/questions")
    ResponseEntity<ClubApplyQuestionsResponse> getApplyQuestions(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 가입 문항을 덮어써서 대체한다.", description = """
        요청에 포함된 문항 목록이 최종 상태가 됩니다.
        - questionId가 있으면 수정
        - questionId가 없으면 생성
        - 요청에 없는 기존 문항은 삭제됩니다.
        - 저장된 문항 목록을 반환합니다.
        
        ## 에러
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
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

    @Operation(summary = "동아리 회비 정보를 조회한다.", description = """
        동아리의 회비 계좌 정보를 조회합니다.

        ## 에러
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        """)
    @PublicApi
    @GetMapping("/{clubId}/fee")
    ResponseEntity<ClubFeeInfoResponse> getFeeInfo(
        @PathVariable(name = "clubId") Integer clubId
    );

    @Operation(summary = "동아리 회비 정보를 덮어써서 대체한다.", description = """
        요청 본문이 최종 상태가 됩니다.
        - 모든 필드를 전달하면 생성/수정합니다.
        - 모든 필드가 null이면 회비 정보를 삭제합니다.
        - 일부 필드가 누락된 경우 에러가 발생합니다.
        
        ## 에러
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_BANK (404): 해당하는 은행을 찾을 수 없습니다.
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - INVALID_REQUEST_BODY (400): 요청 본문의 형식이 올바르지 않거나 필수 값이 누락된 경우
        """)
    @PutMapping("/{clubId}/fee")
    ResponseEntity<ClubFeeInfoResponse> replaceFeeInfo(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubFeeInfoReplaceRequest request,
        @UserId Integer userId
    );
}
