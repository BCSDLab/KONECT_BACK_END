package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.club.dto.ClubMemberSheetSyncResponse;
import gg.agit.konect.domain.club.dto.ClubSheetIdUpdateRequest;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Club - Sheet")
@RequestMapping("/clubs")
public interface ClubMemberSheetApi {

    @Operation(summary = "Register or update the Google Spreadsheet ID for a club.")
    @PutMapping("/{clubId}/sheet")
    ResponseEntity<Void> updateSheetId(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubSheetIdUpdateRequest request,
        @UserId Integer requesterId
    );

    @Operation(summary = "Export club member list to the registered Google Spreadsheet.")
    @PostMapping("/{clubId}/members/sheet-sync")
    ResponseEntity<ClubMemberSheetSyncResponse> syncMembersToSheet(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer requesterId
    );
}
