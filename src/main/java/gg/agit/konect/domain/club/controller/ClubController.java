package gg.agit.konect.domain.club.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.club.dto.ClubApplyQuestionsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubApplyRequest;
import gg.agit.konect.domain.club.dto.ClubCondition;
import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubFeeInfoResponse;
import gg.agit.konect.domain.club.dto.ClubMembersResponse;
import gg.agit.konect.domain.club.dto.ClubMembershipsResponse;
import gg.agit.konect.domain.club.dto.ClubRecruitmentResponse;
import gg.agit.konect.domain.club.dto.ClubsResponse;
import gg.agit.konect.domain.club.service.ClubService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubController implements ClubApi {

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
    public ResponseEntity<ClubMembershipsResponse> getJoinedClubs(@UserId Integer userId) {
        ClubMembershipsResponse response = clubService.getJoinedClubs(userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubMembersResponse> getClubMembers(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    ) {
        ClubMembersResponse response = clubService.getClubMembers(clubId, userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubFeeInfoResponse> applyClub(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubApplyRequest request,
        @UserId Integer userId
    ) {
        ClubFeeInfoResponse response = clubService.applyClub(clubId, userId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubFeeInfoResponse> getFeeInfo(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    ) {
        ClubFeeInfoResponse response = clubService.getFeeInfo(clubId, userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubApplyQuestionsResponse> getApplyQuestions(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    ) {
        ClubApplyQuestionsResponse response = clubService.getApplyQuestions(clubId, userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> updateApplyQuestions(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubApplyQuestionsUpdateRequest request,
        @UserId Integer userId
    ) {
        clubService.updateApplyQuestions(clubId, userId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ClubRecruitmentResponse> getRecruitments(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    ) {
        ClubRecruitmentResponse response = clubService.getRecruitment(clubId, userId);
        return ResponseEntity.ok(response);
    }
}
