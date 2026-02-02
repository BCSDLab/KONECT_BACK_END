package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.club.dto.ClubMemberAddRequest;
import gg.agit.konect.domain.club.dto.MemberPositionChangeRequest;
import gg.agit.konect.domain.club.dto.PresidentTransferRequest;
import gg.agit.konect.domain.club.dto.VicePresidentChangeRequest;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Club - Member: 회원 관리")
@RequestMapping("/clubs")
public interface ClubMemberApi {

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
