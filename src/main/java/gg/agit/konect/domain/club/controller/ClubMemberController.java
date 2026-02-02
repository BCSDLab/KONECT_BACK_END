package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.club.dto.ClubMemberAddRequest;
import gg.agit.konect.domain.club.dto.MemberPositionChangeRequest;
import gg.agit.konect.domain.club.dto.PresidentTransferRequest;
import gg.agit.konect.domain.club.dto.VicePresidentChangeRequest;
import gg.agit.konect.domain.club.service.ClubMemberManagementService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubMemberController implements ClubMemberApi {

    private final ClubMemberManagementService clubMemberManagementService;

    @Override
    public ResponseEntity<Void> changeMemberPosition(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "memberId") Integer memberId,
        @Valid @RequestBody MemberPositionChangeRequest request,
        @UserId Integer userId
    ) {
        clubMemberManagementService.changeMemberPosition(clubId, memberId, userId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> transferPresident(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody PresidentTransferRequest request,
        @UserId Integer userId
    ) {
        clubMemberManagementService.transferPresident(clubId, userId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> changeVicePresident(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody VicePresidentChangeRequest request,
        @UserId Integer userId
    ) {
        clubMemberManagementService.changeVicePresident(clubId, userId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> addMember(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubMemberAddRequest request,
        @UserId Integer userId
    ) {
        clubMemberManagementService.addMember(clubId, userId, request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> removeMember(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "memberId") Integer memberId,
        @UserId Integer userId
    ) {
        clubMemberManagementService.removeMember(clubId, memberId, userId);
        return ResponseEntity.noContent().build();
    }
}
