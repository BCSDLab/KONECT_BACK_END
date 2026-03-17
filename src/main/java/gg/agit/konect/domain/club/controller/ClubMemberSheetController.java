package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.club.dto.ClubMemberSheetSyncResponse;
import gg.agit.konect.domain.club.dto.ClubSheetIdUpdateRequest;
import gg.agit.konect.domain.club.service.ClubMemberSheetService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubMemberSheetController implements ClubMemberSheetApi {

    private final ClubMemberSheetService clubMemberSheetService;

    @Override
    public ResponseEntity<Void> updateSheetId(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubSheetIdUpdateRequest request,
        @UserId Integer requesterId
    ) {
        clubMemberSheetService.updateSheetId(clubId, requesterId, request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<ClubMemberSheetSyncResponse> syncMembersToSheet(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer requesterId
    ) {
        ClubMemberSheetSyncResponse response = clubMemberSheetService.syncMembersToSheet(clubId, requesterId);
        return ResponseEntity.ok(response);
    }
}
