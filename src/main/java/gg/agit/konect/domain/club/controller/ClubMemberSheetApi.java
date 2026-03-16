package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.club.dto.ClubMemberSheetSyncRequest;
import gg.agit.konect.domain.club.dto.ClubMemberSheetSyncResponse;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Club - Sheet: 인명부 시트 동기화")
@RequestMapping("/clubs")
public interface ClubMemberSheetApi {

    @Operation(summary = "동아리 인명부를 구글 스프레드시트로 내보낸다.", description = """
        동아리 운영진 이상만 인명부를 구글 스프레드시트로 내보낼 수 있습니다.
        기존 시트 데이터를 초기화하고 현재 DB 기준 전체 회원 목록을 덮어씁니다.

        ## 시트 컬럼 순서
        이름 | 학번 | 이메일 | 전화번호 | 직책 | 가입일

        ## 사전 조건
        - 서비스 계정 이메일을 해당 스프레드시트에 편집자로 공유해야 합니다.

        ## 에러
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - FAILED_SYNC_GOOGLE_SHEET (500): 구글 스프레드시트 동기화에 실패했습니다.
        """)
    @PostMapping("/{clubId}/members/sheet-sync")
    ResponseEntity<ClubMemberSheetSyncResponse> syncMembersToSheet(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubMemberSheetSyncRequest request,
        @UserId Integer requesterId
    );
}
