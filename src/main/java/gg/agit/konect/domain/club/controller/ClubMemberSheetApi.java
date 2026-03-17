package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gg.agit.konect.domain.club.dto.ClubMemberSheetSyncResponse;
import gg.agit.konect.domain.club.dto.ClubSheetIdUpdateRequest;
import gg.agit.konect.domain.club.enums.ClubSheetSortKey;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Club - Sheet")
@RequestMapping("/clubs")
public interface ClubMemberSheetApi {

    @Operation(
        summary = "구글 스프레드시트 ID 등록 / 수정",
        description = "동아리에서 사용 중인 구글 스프레드시트 ID를 등록하거나 수정합니다. "
            + "등록 시 AI(Claude Haiku)가 시트 상단 10행을 자동으로 분석하여 "
            + "이름·학번·연락처 등 컬럼 위치를 파악하고, 이후 동기화 시 해당 컬럼에만 값을 채웁니다. "
            + "시트 양식이 변경된 경우 이 API를 다시 호출하면 AI가 재분석합니다."
    )
    @PutMapping("/{clubId}/sheet")
    ResponseEntity<Void> updateSheetId(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubSheetIdUpdateRequest request,
        @UserId Integer requesterId
    );

    @Operation(
        summary = "동아리 인명부 스프레드시트 동기화",
        description = "등록된 구글 스프레드시트에 동아리 회원 인명부와 회비 납부 현황을 동기화합니다. "
            + "sortKey로 정렬 기준(NAME, STUDENT_ID, POSITION, JOINED_AT, FEE_PAID)을 지정할 수 있으며, "
            + "ascending으로 오름차순/내림차순을 설정합니다. "
            + "가입 승인·탈퇴·회비 납부 승인 시에도 자동으로 동기화됩니다."
    )
    @PostMapping("/{clubId}/members/sheet-sync")
    ResponseEntity<ClubMemberSheetSyncResponse> syncMembersToSheet(
        @PathVariable(name = "clubId") Integer clubId,
        @RequestParam(name = "sortKey", defaultValue = "POSITION") ClubSheetSortKey sortKey,
        @RequestParam(name = "ascending", defaultValue = "true") boolean ascending,
        @UserId Integer requesterId
    );
}
