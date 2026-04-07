package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.club.dto.ClubMemberSheetSyncResponse;
import gg.agit.konect.domain.club.dto.SheetImportPreviewResponse;
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
        summary = "기존 스프레드시트를 공식 시트로 이관한다",
        description = """
            기존 스프레드시트 URL을 제출하면
            같은 Google Drive 폴더에 KONECT 공식 시트를 만들고 현재 데이터를 복사합니다.
            """
    )
    @PostMapping("/{clubId}/sheet/migrate")
    ResponseEntity<ClubMemberSheetSyncResponse> migrateSheet(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody SheetMigrateRequest request,
        @UserId Integer requesterId
    );

    @Operation(
        summary = "시트 불러오기 전 부원 목록을 미리본다",
        description = """
            스프레드시트 URL을 읽어 등록 예정인 부원 목록을 JSON으로 반환합니다.
            이 API는 데이터를 저장하지 않고 미리보기 용도로만 사용합니다.
            """
    )
    @PostMapping("/{clubId}/sheet/import/preview")
    ResponseEntity<SheetImportPreviewResponse> previewPreMembers(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody SheetImportRequest request,
        @UserId Integer requesterId
    );

    @Operation(
        summary = "스프레드시트에서 사전 등록 부원을 가져온다",
        description = """
            스프레드시트의 부원 정보를 데이터베이스에 반영합니다.
            가입된 사용자는 ClubMember로 바로 등록하고, 미가입 사용자는 ClubPreMember로 저장합니다.
            """
    )
    @PostMapping("/{clubId}/sheet/import")
    ResponseEntity<SheetImportResponse> importPreMembers(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody SheetImportRequest request,
        @UserId Integer requesterId
    );

    @Operation(
        summary = "시트 분석과 사전 등록 부원 가져오기를 한 번에 수행한다",
        description = """
            시트 분석, 시트 등록, 사전 등록 부원 가져오기를 순차적으로 수행합니다.
            기존 PUT /clubs/{clubId}/sheet 이후 POST /clubs/{clubId}/sheet/import 를 호출한 것과 같은 결과입니다.
            """
    )
    @PostMapping("/{clubId}/sheet/import/integrated")
    ResponseEntity<SheetImportResponse> analyzeAndImportPreMembers(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody SheetImportRequest request,
        @UserId Integer requesterId
    );
}
