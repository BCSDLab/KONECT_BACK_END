package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.club.dto.ClubMemberSheetSyncResponse;
import gg.agit.konect.domain.club.dto.SheetImportRequest;
import gg.agit.konect.domain.club.dto.SheetImportResponse;
import gg.agit.konect.domain.club.dto.SheetMigrateRequest;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Club - Sheet")
@RequestMapping("/clubs")
public interface ClubSheetMigrationApi {

    @Operation(
        summary = "기존 스프레드시트 → 팀 양식으로 이관",
        description = "동아리가 기존에 사용하던 스프레드시트 URL을 제출하면, "
            + "AI가 데이터를 분석하여 KONECT 팀이 마련한 표준 양식 파일로 복사합니다. "
            + "새 파일은 기존 URL과 동일한 Google Drive 폴더에 생성됩니다. "
            + "이후 동기화는 새로 생성된 파일 기준으로 진행됩니다."
    )
    @PostMapping("/{clubId}/sheet/migrate")
    ResponseEntity<ClubMemberSheetSyncResponse> migrateSheet(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody SheetMigrateRequest request,
        @UserId Integer requesterId
    );

    @Operation(
        summary = "기존 스프레드시트에서 사전 회원 가져오기",
        description = "동아리가 기존에 관리하던 스프레드시트의 인명부를 읽어 "
            + "DB에 사전 회원(ClubPreMember)으로 등록합니다. "
            + "AI가 헤더를 자동 분석하며, 이미 등록된 회원(이름+학번 중복)은 건너뜁니다."
    )
    @PostMapping("/{clubId}/sheet/import")
    ResponseEntity<SheetImportResponse> importPreMembers(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody SheetImportRequest request,
        @UserId Integer requesterId
    );
}
