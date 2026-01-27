package gg.agit.konect.domain.club.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.club.dto.ClubAppliedClubsResponse;
import gg.agit.konect.domain.club.dto.ClubBasicInfoUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubCondition;
import gg.agit.konect.domain.club.dto.ClubCreateRequest;
import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubDetailUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubMemberCondition;
import gg.agit.konect.domain.club.dto.ClubMembersResponse;
import gg.agit.konect.domain.club.dto.ClubMembershipsResponse;
import gg.agit.konect.domain.club.dto.ClubProfileUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubTagsResponse;
import gg.agit.konect.domain.club.dto.ClubsResponse;
import gg.agit.konect.domain.club.service.ClubService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubBasicController implements ClubBasicApi {

    private final ClubService clubService;

    @Override
    public ResponseEntity<ClubsResponse> getClubs(
        @Valid @ParameterObject @ModelAttribute ClubCondition condition,
        @UserId Integer userId
    ) {
        ClubsResponse response = clubService.getClubs(condition, userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubDetailResponse> getClubDetail(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    ) {
        ClubDetailResponse response = clubService.getClubDetail(clubId, userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubDetailResponse> createClub(
        @Valid @RequestBody ClubCreateRequest request,
        @UserId Integer userId
    ) {
        ClubDetailResponse response = clubService.createClub(userId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> updateProfile(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubProfileUpdateRequest request,
        @UserId Integer userId
    ) {
        clubService.updateProfile(clubId, userId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> updateDetails(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubDetailUpdateRequest request,
        @UserId Integer userId
    ) {
        clubService.updateDetails(clubId, userId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> updateBasicInfo(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubBasicInfoUpdateRequest request,
        @UserId Integer userId
    ) {
        clubService.updateBasicInfo(clubId, userId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ClubTagsResponse> getTags() {
        ClubTagsResponse response = clubService.getTags();
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubMembershipsResponse> getJoinedClubs(@UserId Integer userId) {
        ClubMembershipsResponse response = clubService.getJoinedClubs(userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubMembershipsResponse> getManagedClubs(@UserId Integer userId) {
        ClubMembershipsResponse response = clubService.getManagedClubs(userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubAppliedClubsResponse> getAppliedClubs(@UserId Integer userId) {
        ClubAppliedClubsResponse response = clubService.getAppliedClubs(userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubMembersResponse> getClubMembers(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @ParameterObject @ModelAttribute ClubMemberCondition condition,
        @UserId Integer userId
    ) {
        ClubMembersResponse response = clubService.getClubMembers(clubId, userId, condition);
        return ResponseEntity.ok(response);
    }
}
