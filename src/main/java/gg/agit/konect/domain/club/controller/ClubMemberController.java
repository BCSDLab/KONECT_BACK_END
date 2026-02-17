package gg.agit.konect.domain.club.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.club.dto.ClubPreMemberAddRequest;
import gg.agit.konect.domain.club.dto.ClubPreMemberAddResponse;
import gg.agit.konect.domain.club.dto.ClubMemberChangesResponse;
import gg.agit.konect.domain.club.dto.ClubMemberResponse;
import gg.agit.konect.domain.club.dto.ClubPreMembersResponse;
import gg.agit.konect.domain.club.dto.MemberPositionChangeRequest;
import gg.agit.konect.domain.club.dto.PresidentTransferRequest;
import gg.agit.konect.domain.club.dto.VicePresidentChangeRequest;
import gg.agit.konect.domain.club.model.ClubMember;
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
    public ResponseEntity<ClubMemberResponse> changeMemberPosition(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "userId") Integer targetUserId,
        @Valid @RequestBody MemberPositionChangeRequest request,
        @UserId Integer requesterId
    ) {
        ClubMember changedMember = clubMemberManagementService.changeMemberPosition(
            clubId, targetUserId, requesterId, request
        );
        return ResponseEntity.ok(ClubMemberResponse.from(changedMember));
    }

    @Override
    public ResponseEntity<ClubMemberChangesResponse> transferPresident(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody PresidentTransferRequest request,
        @UserId Integer userId
    ) {
        List<ClubMember> changedMembers = clubMemberManagementService.transferPresident(
            clubId, userId, request
        );
        return ResponseEntity.ok(ClubMemberChangesResponse.from(changedMembers));
    }

    @Override
    public ResponseEntity<ClubMemberChangesResponse> changeVicePresident(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody VicePresidentChangeRequest request,
        @UserId Integer userId
    ) {
        List<ClubMember> changedMembers = clubMemberManagementService.changeVicePresident(
            clubId, userId, request
        );
        return ResponseEntity.ok(ClubMemberChangesResponse.from(changedMembers));
    }

    @Override
    public ResponseEntity<ClubPreMemberAddResponse> addPreMember(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubPreMemberAddRequest request,
        @UserId Integer userId
    ) {
        ClubPreMemberAddResponse response = clubMemberManagementService.addPreMember(clubId, userId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubPreMembersResponse> getPreMembers(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    ) {
        ClubPreMembersResponse response = clubMemberManagementService.getPreMembers(clubId, userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> removePreMember(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "preMemberId") Integer preMemberId,
        @UserId Integer requesterId
    ) {
        clubMemberManagementService.removePreMember(clubId, preMemberId, requesterId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> removeMember(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "userId") Integer targetUserId,
        @UserId Integer requesterId
    ) {
        clubMemberManagementService.removeMember(clubId, targetUserId, requesterId);
        return ResponseEntity.noContent().build();
    }
}
