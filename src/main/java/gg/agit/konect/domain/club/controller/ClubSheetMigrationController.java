package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.club.dto.ClubMemberSheetSyncResponse;
import gg.agit.konect.domain.club.dto.SheetImportRequest;
import gg.agit.konect.domain.club.dto.SheetImportResponse;
import gg.agit.konect.domain.club.dto.SheetMigrateRequest;
import gg.agit.konect.domain.club.service.ClubSheetIntegratedService;
import gg.agit.konect.domain.club.service.SheetImportService;
import gg.agit.konect.domain.club.service.SheetMigrationService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubSheetMigrationController implements ClubSheetMigrationApi {

    private final SheetMigrationService sheetMigrationService;
    private final SheetImportService sheetImportService;
    private final ClubSheetIntegratedService clubSheetIntegratedService;

    @Override
    public ResponseEntity<ClubMemberSheetSyncResponse> migrateSheet(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody SheetMigrateRequest request,
        @UserId Integer requesterId
    ) {
        String newSpreadsheetId = sheetMigrationService.migrateToTemplate(
            clubId, requesterId, request.sourceSpreadsheetUrl()
        );
        return ResponseEntity.ok(ClubMemberSheetSyncResponse.of(0, newSpreadsheetId));
    }

    @Override
    public ResponseEntity<SheetImportResponse> importPreMembers(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody SheetImportRequest request,
        @UserId Integer requesterId
    ) {
        SheetImportResponse response = sheetImportService.importPreMembersFromSheet(
            clubId, requesterId, request.spreadsheetUrl()
        );
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<SheetImportResponse> analyzeAndImportPreMembers(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody SheetImportRequest request,
        @UserId Integer requesterId
    ) {
        SheetImportResponse response = clubSheetIntegratedService.analyzeAndImportPreMembers(
            clubId, requesterId, request.spreadsheetUrl()
        );
        return ResponseEntity.ok(response);
    }
}
