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

import gg.agit.konect.domain.club.dto.ClubAppliedClubsResponse;
import gg.agit.konect.domain.club.dto.ClubBasicInfoUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubCondition;
import gg.agit.konect.domain.club.dto.ClubCreateRequest;
import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubMemberCondition;
import gg.agit.konect.domain.club.dto.ClubMembersResponse;
import gg.agit.konect.domain.club.dto.ClubMembershipsResponse;
import gg.agit.konect.domain.club.dto.ClubsResponse;
import gg.agit.konect.domain.club.dto.MyManagedClubResponse;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Club - Basic: 기본 관리")
@RequestMapping("/clubs")
public interface ClubBasicApi {

    @Operation(summary = "페이지 네이션으로 동아리 리스트를 조회한다.", description = """
        - isRecruiting가 true일 경우, 모집 중인 동아리만 조회하며 모집일(마감일)이 빠른 순으로 정렬됩니다.
        - isRecruiting가 false일 경우, 전체 동아리를 조회하되 모집 중인 동아리를 먼저 보여줍니다.
        - status은 BEFORE(모집 전), ONGOING(모집 중), CLOSED(모집 마감)으로 반환됩니다.
        
        ## 에러
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        """)
    @GetMapping
    ResponseEntity<ClubsResponse> getClubs(
        @Valid @ParameterObject @ModelAttribute ClubCondition condition,
        @UserId Integer userId
    );

    @Operation(summary = "동아리의 상세 정보를 조회한다.", description = """
        - recruitmentStatus는 모집 기간에 따라 BEFORE(모집 전), ONGOING(모집 중), CLOSED(모집 마감)으로 반환됩니다.
        - 모집 일정 데이터가 존재하지 않는다면 CLOSED(모집 마감)으로 간주되며, startAt, endAt은 null로 반환됩니다.
        - 동아리 멤버이거나 지원 이력이 존재할 경우 isApplied는 true로 반환됩니다.
        
        ## 에러
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        """)
    @GetMapping("/{clubId}")
    ResponseEntity<ClubDetailResponse> getClubDetail(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    );

    @Operation(summary = "새로운 동아리를 생성한다 (어드민 전용).", description = """
        어드민만 새로운 동아리를 생성할 수 있습니다.
        요청 본문의 presidentUserId로 지정된 유저를 회장으로 등록하며, 해당 유저의 대학교에 동아리가 생성됩니다.

        ## 에러
        - FORBIDDEN_ROLE_ACCESS (403): 어드민 권한이 없습니다.
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        """)
    @PostMapping
    ResponseEntity<ClubDetailResponse> createClub(
        @Valid @RequestBody ClubCreateRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 정보를 수정한다.", description = """
        동아리 회장 또는 부회장만 동아리 정보를 수정할 수 있습니다.
        수정 가능 항목: 한 줄 소개, 로고 이미지, 동방 위치, 상세 소개
        동아리명과 분과는 수정할 수 없으며, 변경이 필요한 경우 문의하기를 통해 어드민에게 요청하세요.

        ## 에러
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        """)
    @PutMapping("/{clubId}/info")
    ResponseEntity<Void> updateInfo(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubUpdateRequest request,
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

    @Operation(summary = "관리 중인 동아리의 상세 정보를 조회한다.", description = """
        동아리 관리자(회장, 부회장, 운영진)만 조회할 수 있습니다.
        
        ## 에러
        - NOT_FOUND_CLUB_MEMBER (404): 해당하는 동아리 원을 찾을 수 없습니다.
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        """)
    @GetMapping("/managed/{clubId}")
    ResponseEntity<MyManagedClubResponse> getManagedClubDetail(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    );

    @Operation(summary = "가입 승인 대기 중인 동아리 리스트를 조회한다.")
    @GetMapping("/applied")
    ResponseEntity<ClubAppliedClubsResponse> getAppliedClubs(
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
}
