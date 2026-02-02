package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.club.dto.ClubPositionCreateRequest;
import gg.agit.konect.domain.club.dto.ClubPositionUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubPositionsResponse;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Club - Position: 직책 관리")
@RequestMapping("/clubs")
public interface ClubPositionApi {

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
}
