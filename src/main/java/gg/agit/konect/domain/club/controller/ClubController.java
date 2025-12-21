package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.club.dto.ClubApplyQuestionsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyRequest;
import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubFeeInfoResponse;
import gg.agit.konect.domain.club.dto.ClubMembersResponse;
import gg.agit.konect.domain.club.dto.ClubsResponse;
import gg.agit.konect.domain.club.dto.JoinedClubsResponse;
import gg.agit.konect.domain.club.service.ClubService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubController implements ClubApi {

    private final ClubService clubService;

    @GetMapping
    public ResponseEntity<ClubsResponse> getClubs(
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "10", required = false) Integer limit,
        @RequestParam(name = "query", defaultValue = "", required = false) String query,
        @RequestParam(name = "isRecruiting", defaultValue = "false", required = false) Boolean isRecruiting
    ) {
        ClubsResponse response = clubService.getClubs(page, limit, query, isRecruiting);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{clubId}")
    public ResponseEntity<ClubDetailResponse> getClubDetail(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    ) {
        ClubDetailResponse response = clubService.getClubDetail(clubId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/joined")
    public ResponseEntity<JoinedClubsResponse> getJoinedClubs(@UserId Integer userId) {
        JoinedClubsResponse response = clubService.getJoinedClubs(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{clubId}/members")
    public ResponseEntity<ClubMembersResponse> getClubMembers(
        @PathVariable(name = "clubId") Integer clubId
    ) {
        ClubMembersResponse response = clubService.getClubMembers(clubId);
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
        @PathVariable(name = "clubId") Integer clubId
    ) {
        ClubApplyQuestionsResponse response = clubService.getApplyQuestions(clubId);
        return ResponseEntity.ok(response);
    }
}
