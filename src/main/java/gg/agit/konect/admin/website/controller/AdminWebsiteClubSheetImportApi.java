package gg.agit.konect.admin.website.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportConfirmRequest;
import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportPreviewResponse;
import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportRequest;
import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Admin) Website Club Sheet Import", description = "konect.space 대학별 동아리 목록 시트 등록 API")
@RequestMapping("/admin/konect/universities")
public interface AdminWebsiteClubSheetImportApi {

    @Operation(
        summary = "Google Sheets 동아리 등록 양식을 읽고 미리보기 JSON을 반환한다.",
        description = """
            고정된 작성 시트 양식의 A~F 컬럼을 읽어 KONECT 웹사이트용 동아리 JSON을 생성합니다.
            이 API는 DB에 저장하지 않고, 사용자가 확인/수정할 수 있는 중간 결과만 반환합니다.
            """
    )
    @PostMapping("/{universityId}/clubs/sheet/import/preview")
    ResponseEntity<AdminWebsiteClubSheetImportPreviewResponse> previewClubs(
        @PathVariable(name = "universityId") Integer universityId,
        @Valid @RequestBody AdminWebsiteClubSheetImportRequest request
    );

    @Operation(
        summary = "미리보기 JSON을 최종 동아리 목록으로 저장한다.",
        description = """
            preview 응답을 그대로 보내거나 수정한 뒤 보내면 web_club에 저장합니다.
            enabled=false인 항목과 이미 같은 대학에 등록된 같은 이름의 동아리는 저장하지 않습니다.
            """
    )
    @PostMapping("/{universityId}/clubs/sheet/import/confirm")
    ResponseEntity<AdminWebsiteClubSheetImportResponse> confirmImport(
        @PathVariable(name = "universityId") Integer universityId,
        @Valid @RequestBody AdminWebsiteClubSheetImportConfirmRequest request
    );
}
