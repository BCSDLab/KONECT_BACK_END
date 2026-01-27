package gg.agit.konect.domain.club.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.club.dto.ClubApplicationAnswersResponse;
import gg.agit.konect.domain.club.dto.ClubAppliedClubsResponse;
import gg.agit.konect.domain.club.dto.ClubApplicationsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyRequest;
import gg.agit.konect.domain.club.dto.ClubBasicInfoUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubCondition;
import gg.agit.konect.domain.club.dto.ClubCreateRequest;
import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubDetailUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubFeeInfoReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubFeeInfoResponse;
import gg.agit.konect.domain.club.dto.ClubMemberAddRequest;
import gg.agit.konect.domain.club.dto.ClubMemberCondition;
import gg.agit.konect.domain.club.dto.ClubMembersResponse;
import gg.agit.konect.domain.club.dto.ClubMembershipsResponse;
import gg.agit.konect.domain.club.dto.ClubPositionCreateRequest;
import gg.agit.konect.domain.club.dto.ClubPositionUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubPositionsResponse;
import gg.agit.konect.domain.club.dto.ClubProfileUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubRecruitmentCreateRequest;
import gg.agit.konect.domain.club.dto.ClubRecruitmentResponse;
import gg.agit.konect.domain.club.dto.ClubRecruitmentUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubTagsResponse;
import gg.agit.konect.domain.club.dto.ClubsResponse;
import gg.agit.konect.domain.club.dto.MemberPositionChangeRequest;
import gg.agit.konect.domain.club.dto.PresidentTransferRequest;
import gg.agit.konect.domain.club.dto.VicePresidentChangeRequest;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Club: 동아리", description = "동아리 API")
@RequestMapping("/clubs")
public interface ClubApi {

    @Operation(summary = "페이지 네이션으로 동아리 리스트를 조회한다.", description = """
        - isRecruiting가 true일 경우, 모집 중인 동아리만 조회하며 모집일(마감일)이 빠른 순으로 정렬됩니다.
        - isRecruiting가 false일 경우, 전체 동아리를 조회하되 모집 중인 동아리를 먼저 보여줍니다.
        - status은 BEFORE(모집 전), ONGOING(모집 중), CLOSED(모집 마감)으로 반환됩니다.
        - isAlwaysRecruiting는 상시 모집 여부입니다.
        - applicationDeadline는 모집 마감일(endDate)이며, 모집 공고가 없거나 상시 모집이면 null로 반환됩니다.
        - isPendingApproval은 가입 승인 대기 중 여부이며, 지원 내역이 존재하지만 아직 멤버가 아닌 경우 true로 반환됩니다.
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

    @Operation(summary = "새로운 동아리를 생성한다.", description = """
        새로운 동아리를 생성하고, 생성한 사용자를 회장으로 등록합니다.

        ## 에러
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        """)
    @PostMapping
    ResponseEntity<ClubDetailResponse> createClub(
        @Valid @RequestBody ClubCreateRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 프로필을 수정한다.", description = """
        동아리 회장 또는 부회장만 동아리 프로필을 수정할 수 있습니다.
        수정 가능 항목: 한 줄 소개, 로고 이미지, 태그
        동아리명과 분과는 수정할 수 없으며, 변경이 필요한 경우 문의하기를 통해 어드민에게 요청하세요.

        ## 에러
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        """)
    @PutMapping("/{clubId}/profile")
    ResponseEntity<Void> updateProfile(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubProfileUpdateRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 상세정보를 수정한다.", description = """
        동아리 회장 또는 부회장만 동아리 상세정보를 수정할 수 있습니다.
        수정 가능 항목: 동방 위치, 상세 소개

        ## 에러
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        """)
    @PutMapping("/{clubId}/details")
    ResponseEntity<Void> updateDetails(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubDetailUpdateRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 기본정보를 수정한다 (어드민 전용).", description = """
        어드민만 동아리 기본정보를 수정할 수 있습니다.
        수정 가능 항목: 동아리명, 분과
        일반 관리자는 이 API를 사용할 수 없으며, 변경이 필요한 경우 문의하기를 통해 어드민에게 요청하세요.

        ## 에러
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 어드민 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        """)
    @PutMapping("/{clubId}/basic-info")
    ResponseEntity<Void> updateBasicInfo(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubBasicInfoUpdateRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "사용 가능한 전체 태그 목록을 조회한다.")
    @GetMapping("/tags")
    ResponseEntity<ClubTagsResponse> getTags();

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

    @Operation(summary = "가입 승인 대기 중인 동아리 리스트를 조회한다.")
    @GetMapping("/applied")
    ResponseEntity<ClubAppliedClubsResponse> getAppliedClubs(
        @UserId Integer userId
    );

    @Operation(summary = "동아리 지원 내역을 조회한다.", description = """
         - 동아리 관리자만 해당 동아리의 지원 내역을 조회할 수 있습니다.
         - 현재 지정된 모집 일정 범위에 지원한 내역만 볼 수 있습니다.
         - 상시 모집의 경우 모든 내역을 봅니다.
                
        ## 에러
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_CLUB_RECRUITMENT (404): 동아리 모집 공고를 찾을 수 없습니다.
        """)
    @GetMapping("/{clubId}/applications")
    ResponseEntity<ClubApplicationsResponse> getClubApplications(
        @PathVariable(name = "clubId") Integer clubId,
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
        승인 시 지원자는 동아리 회원으로 등록되고, 지원 내역은 삭제됩니다.
        승인 시 회비는 지원 절차에서 이미 낸 것으로 간주됩니다.
    
        ## 에러
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_CLUB_APPLY (404): 동아리 지원 내역을 찾을 수 없습니다.
        - NOT_FOUND_CLUB_POSITION (404): 동아리 직책을 찾을 수 없습니다.
        - ALREADY_CLUB_MEMBER (409): 이미 동아리 회원입니다.
        """)
    @PostMapping("/{clubId}/applications/{applicationId}/approve")
    ResponseEntity<Void> approveClubApplication(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "applicationId") Integer applicationId,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 멤버 리스트를 조회한다.", description = """
        동아리 회원만 멤버 리스트를 조회할 수 있습니다.
        positionGroup 파라미터로 특정 직책 그룹의 회원만 필터링할 수 있습니다.

        ## 에러
        - FORBIDDEN_CLUB_MEMBER_ACCESS (403): 동아리 멤버 조회 권한이 없습니다.
        """)
    @GetMapping("/{clubId}/members")
    ResponseEntity<ClubMembersResponse> getClubMembers(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @ParameterObject @ModelAttribute ClubMemberCondition condition,
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

    @Operation(summary = "동아리 모집 정보를 수정한다.", description = """
        동아리 회장 또는 부회장만 모집 공고를 수정할 수 있습니다.
                
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

    @Operation(summary = "동아리 직책 목록을 조회한다.", description = """
        동아리의 모든 직책을 우선순위 순으로 조회합니다.
        각 직책의 회원 수, 수정/삭제 가능 여부도 함께 반환됩니다.

        ## 에러
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        """)
    @GetMapping("/{clubId}/positions")
    ResponseEntity<ClubPositionsResponse> getClubPositions(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 직책을 생성한다.", description = """
        동아리 회장 또는 부회장만 직책을 생성할 수 있습니다.
        PRESIDENT와 VICE_PRESIDENT 직책은 생성할 수 없으며, MANAGER 또는 MEMBER 그룹의 직책만 생성 가능합니다.

        ## 에러
        - POSITION_NAME_DUPLICATED (400): 동일한 직책 이름이 이미 존재합니다.
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        """)
    @PostMapping("/{clubId}/positions")
    ResponseEntity<ClubPositionsResponse> createClubPosition(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubPositionCreateRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 직책의 이름을 수정한다.", description = """
        동아리 회장 또는 부회장만 직책 이름을 수정할 수 있습니다.
        PRESIDENT와 VICE_PRESIDENT 직책의 이름은 변경할 수 없습니다.

        ## 에러
        - POSITION_NAME_DUPLICATED (400): 동일한 직책 이름이 이미 존재합니다.
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - FORBIDDEN_POSITION_NAME_CHANGE (403): 해당 직책의 이름은 변경할 수 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_CLUB_POSITION (404): 동아리 직책을 찾을 수 없습니다.
        """)
    @PatchMapping("/{clubId}/positions/{positionId}")
    ResponseEntity<ClubPositionsResponse> updateClubPositionName(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "positionId") Integer positionId,
        @Valid @RequestBody ClubPositionUpdateRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 직책을 삭제한다.", description = """
        동아리 회장 또는 부회장만 직책을 삭제할 수 있습니다.
        PRESIDENT와 VICE_PRESIDENT 직책은 삭제할 수 없습니다.
        해당 직책을 사용 중인 회원이 없어야 하며, 해당 그룹에 최소 2개의 직책이 있어야 삭제 가능합니다.

        ## 에러
        - CANNOT_DELETE_ESSENTIAL_POSITION (400): 필수 직책은 삭제할 수 없습니다.
        - POSITION_IN_USE (400): 해당 직책을 사용 중인 회원이 있어 삭제할 수 없습니다.
        - INSUFFICIENT_POSITION_COUNT (400): 해당 그룹에 최소 2개의 직책이 있어야 삭제 가능합니다.
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_CLUB_POSITION (404): 동아리 직책을 찾을 수 없습니다.
        """)
    @DeleteMapping("/{clubId}/positions/{positionId}")
    ResponseEntity<Void> deleteClubPosition(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "positionId") Integer positionId,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 회원의 직책을 변경한다.", description = """
        동아리 회장 또는 부회장만 회원의 직책을 변경할 수 있습니다.
        자기 자신의 직책은 변경할 수 없으며, 상위 직급만 하위 직급의 회원을 관리할 수 있습니다.

        ## 에러
        - CANNOT_CHANGE_OWN_POSITION (400): 자기 자신의 직책은 변경할 수 없습니다.
        - CANNOT_MANAGE_HIGHER_POSITION (400): 자신보다 높은 직급의 회원은 관리할 수 없습니다.
        - VICE_PRESIDENT_ALREADY_EXISTS (409): 부회장은 이미 존재합니다.
        - MANAGER_LIMIT_EXCEEDED (400): 운영진은 최대 20명까지 임명 가능합니다.
        - FORBIDDEN_MEMBER_POSITION_CHANGE (403): 회원 직책 변경 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_CLUB_MEMBER (404): 동아리 회원을 찾을 수 없습니다.
        - NOT_FOUND_CLUB_POSITION (404): 동아리 직책을 찾을 수 없습니다.
        """)
    @PatchMapping("/{clubId}/members/{memberId}/position")
    ResponseEntity<Void> changeMemberPosition(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "memberId") Integer memberId,
        @Valid @RequestBody MemberPositionChangeRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 회장 권한을 위임한다.", description = """
        현재 회장만 회장 권한을 다른 회원에게 위임할 수 있습니다.
        회장 위임 시 현재 회장은 일반회원으로 강등됩니다.

        ## 에러
        - ILLEGAL_ARGUMENT (400): 자기 자신에게는 위임할 수 없습니다.
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 회장 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_CLUB_MEMBER (404): 동아리 회원을 찾을 수 없습니다.
        - NOT_FOUND_CLUB_PRESIDENT (404): 동아리 회장을 찾을 수 없습니다.
        - NOT_FOUND_CLUB_POSITION (404): 동아리 직책을 찾을 수 없습니다.
        """)
    @PostMapping("/{clubId}/president/transfer")
    ResponseEntity<Void> transferPresident(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody PresidentTransferRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 부회장을 변경한다.", description = """
        동아리 회장만 부회장을 임명하거나 해제할 수 있습니다.
        vicePresidentUserId가 null이면 부회장을 해제하고, 값이 있으면 해당 회원을 부회장으로 임명합니다.

        ## 에러
        - CANNOT_CHANGE_OWN_POSITION (400): 자기 자신을 부회장으로 임명할 수 없습니다.
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 회장 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_CLUB_MEMBER (404): 동아리 회원을 찾을 수 없습니다.
        - NOT_FOUND_CLUB_POSITION (404): 동아리 직책을 찾을 수 없습니다.
        """)
    @PatchMapping("/{clubId}/vice-president")
    ResponseEntity<Void> changeVicePresident(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody VicePresidentChangeRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "동아리에 회원을 직접 추가한다.", description = """
        동아리 회장 또는 부회장만 회원을 직접 추가할 수 있습니다.
        회장 직책으로는 추가할 수 없으며, 부회장과 운영진은 인원 제한이 있습니다.

        ## 에러
        - ALREADY_CLUB_MEMBER (409): 이미 동아리 회원입니다.
        - VICE_PRESIDENT_ALREADY_EXISTS (409): 부회장은 이미 존재합니다.
        - MANAGER_LIMIT_EXCEEDED (400): 운영진은 최대 20명까지 임명 가능합니다.
        - FORBIDDEN_MEMBER_POSITION_CHANGE (403): 회원 추가 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        - NOT_FOUND_CLUB_POSITION (404): 동아리 직책을 찾을 수 없습니다.
        """)
    @PostMapping("/{clubId}/members")
    ResponseEntity<Void> addMember(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubMemberAddRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 회원을 강제 탈퇴시킨다.", description = """
        동아리 회장 또는 부회장만 회원을 강제 탈퇴시킬 수 있습니다.
        일반회원만 강제 탈퇴 가능하며, 부회장이나 운영진은 먼저 직책을 변경한 후 탈퇴시켜야 합니다.

        ## 에러
        - CANNOT_REMOVE_SELF (400): 자기 자신을 강제 탈퇴시킬 수 없습니다.
        - CANNOT_REMOVE_NON_MEMBER (400): 일반회원만 강제 탈퇴할 수 있습니다.
        - CANNOT_DELETE_CLUB_PRESIDENT (400): 회장은 강제 탈퇴시킬 수 없습니다.
        - CANNOT_MANAGE_HIGHER_POSITION (400): 자신보다 높은 직급의 회원은 관리할 수 없습니다.
        - FORBIDDEN_MEMBER_POSITION_CHANGE (403): 회원 관리 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_CLUB_MEMBER (404): 동아리 회원을 찾을 수 없습니다.
        """)
    @DeleteMapping("/{clubId}/members/{memberId}")
    ResponseEntity<Void> removeMember(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "memberId") Integer memberId,
        @UserId Integer userId
    );
}
