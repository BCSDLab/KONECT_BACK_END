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
        summary = "Migrate an existing spreadsheet into the official sheet",
        description = """
            When the existing spreadsheet URL is provided,
            KONECT creates the official sheet in the same Drive folder and copies the current data.
            """
    )
    @PostMapping("/{clubId}/sheet/migrate")
    ResponseEntity<ClubMemberSheetSyncResponse> migrateSheet(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody SheetMigrateRequest request,
        @UserId Integer requesterId
    );

    @Operation(
        summary = "Preview sheet members before importing",
        description = """
            Reads the spreadsheet URL and returns the member list that would be imported as JSON.
            This endpoint does not persist data and is intended only for preview usage.
            """
    )
    @PostMapping("/{clubId}/sheet/import/preview")
    ResponseEntity<SheetImportPreviewResponse> previewPreMembers(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody SheetImportRequest request,
        @UserId Integer requesterId
    );

    @Operation(
        summary = "Import pre-members from a spreadsheet",
        description = """
            Reflects member information from the spreadsheet into the database.
            Existing users are registered directly as ClubMember, and others are stored as ClubPreMember.
            """
    )
    @PostMapping("/{clubId}/sheet/import")
    ResponseEntity<SheetImportResponse> importPreMembers(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody SheetImportRequest request,
        @UserId Integer requesterId
    );

    @Operation(
        summary = "Analyze the sheet and import pre-members in one step",
        description = """
            Runs sheet analysis, sheet registration, and pre-member import in sequence.
            The result is equivalent to calling PUT /clubs/{clubId}/sheet and then POST /clubs/{clubId}/sheet/import.
            """
    )
    @PostMapping("/{clubId}/sheet/import/integrated")
    ResponseEntity<SheetImportResponse> analyzeAndImportPreMembers(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody SheetImportRequest request,
        @UserId Integer requesterId
    );
}
