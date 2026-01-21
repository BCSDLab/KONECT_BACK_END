package gg.agit.konect.domain.club.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.club.dto.ClubApplicationAnswersResponse;
import gg.agit.konect.domain.club.dto.ClubApplicationsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyRequest;
import gg.agit.konect.domain.club.dto.ClubCondition;
import gg.agit.konect.domain.club.dto.ClubCreateRequest;
import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubFeeInfoReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubFeeInfoResponse;
import gg.agit.konect.domain.club.dto.ClubMemberAddRequest;
import gg.agit.konect.domain.club.dto.ClubMemberCondition;
import gg.agit.konect.domain.club.dto.ClubMembersResponse;
import gg.agit.konect.domain.club.dto.ClubMembershipsResponse;
import gg.agit.konect.domain.club.dto.ClubPositionCreateRequest;
import gg.agit.konect.domain.club.dto.ClubPositionUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubPositionsResponse;
import gg.agit.konect.domain.club.dto.ClubRecruitmentCreateRequest;
import gg.agit.konect.domain.club.dto.ClubRecruitmentResponse;
import gg.agit.konect.domain.club.dto.ClubRecruitmentUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubsResponse;
import gg.agit.konect.domain.club.dto.MemberPositionChangeRequest;
import gg.agit.konect.domain.club.dto.PresidentTransferRequest;
import gg.agit.konect.domain.club.dto.VicePresidentChangeRequest;
import gg.agit.konect.domain.club.service.ClubMemberManagementService;
import gg.agit.konect.domain.club.service.ClubPositionService;
import gg.agit.konect.domain.club.service.ClubService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubController implements ClubApi {

    private final ClubService clubService;
    private final ClubPositionService clubPositionService;
    private final ClubMemberManagementService clubMemberManagementService;

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
    public ResponseEntity<ClubDetailResponse> updateClub(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubUpdateRequest request,
        @UserId Integer userId
    ) {
        ClubDetailResponse response = clubService.updateClub(clubId, userId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubMembershipsResponse> getJoinedClubs(@UserId Integer userId) {
        ClubMembershipsResponse response = clubService.getJoinedClubs(userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubMembershipsResponse> getManagedClubs(
        @UserId Integer userId
    ) {
        ClubMembershipsResponse response = clubService.getManagedClubs(userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubApplicationsResponse> getClubApplications(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    ) {
        ClubApplicationsResponse response = clubService.getClubApplications(clubId, userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubApplicationAnswersResponse> getClubApplicationAnswers(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "applicationId") Integer applicationId,
        @UserId Integer userId
    ) {
        ClubApplicationAnswersResponse response = clubService.getClubApplicationAnswers(
            clubId,
            applicationId,
            userId
        );

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
    public ResponseEntity<ClubFeeInfoResponse> replaceFeeInfo(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubFeeInfoReplaceRequest request,
        @UserId Integer userId
    ) {
        ClubFeeInfoResponse response = clubService.replaceFeeInfo(clubId, userId, request);
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
    public ResponseEntity<ClubApplyQuestionsResponse> replaceApplyQuestions(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubApplyQuestionsReplaceRequest request,
        @UserId Integer userId
    ) {
        ClubApplyQuestionsResponse response = clubService.replaceApplyQuestions(clubId, userId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubRecruitmentResponse> getRecruitments(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    ) {
        ClubRecruitmentResponse response = clubService.getRecruitment(clubId, userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> createRecruitment(
        @RequestBody @Valid ClubRecruitmentCreateRequest request,
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    ) {
        clubService.createRecruitment(clubId, userId, request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> updateRecruitment(
        @Valid @RequestBody ClubRecruitmentUpdateRequest request,
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    ) {
        clubService.updateRecruitment(clubId, userId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ClubPositionsResponse> getClubPositions(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    ) {
        ClubPositionsResponse response = clubPositionService.getClubPositions(clubId, userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubPositionsResponse> createClubPosition(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubPositionCreateRequest request,
        @UserId Integer userId
    ) {
        ClubPositionsResponse response = clubPositionService.createClubPosition(clubId, userId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubPositionsResponse> updateClubPositionName(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "positionId") Integer positionId,
        @Valid @RequestBody ClubPositionUpdateRequest request,
        @UserId Integer userId
    ) {
        ClubPositionsResponse response = clubPositionService.updateClubPositionName(
            clubId, positionId, userId, request
        );
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> deleteClubPosition(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "positionId") Integer positionId,
        @UserId Integer userId
    ) {
        clubPositionService.deleteClubPosition(clubId, positionId, userId);
        return ResponseEntity.noContent().build();
    }

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
