package gg.agit.konect.domain.club.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.club.dto.ClubApplicationAnswersResponse;
import gg.agit.konect.domain.club.dto.ClubApplicationCondition;
import gg.agit.konect.domain.club.dto.ClubApplicationsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyRequest;
import gg.agit.konect.domain.club.dto.ClubFeeInfoReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubFeeInfoResponse;
import gg.agit.konect.domain.club.service.ClubService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubApplicationController implements ClubApplicationApi {

    private final ClubService clubService;

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
    public ResponseEntity<ClubApplicationsResponse> getClubApplications(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @ParameterObject @ModelAttribute ClubApplicationCondition condition,
        @UserId Integer userId
    ) {
        ClubApplicationsResponse response = clubService.getClubApplications(clubId, userId, condition);
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
    public ResponseEntity<Void> approveClubApplication(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "applicationId") Integer applicationId,
        @UserId Integer userId
    ) {
        clubService.approveClubApplication(clubId, applicationId, userId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> rejectClubApplication(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "applicationId") Integer applicationId,
        @UserId Integer userId
    ) {
        clubService.rejectClubApplication(clubId, applicationId, userId);
        return ResponseEntity.ok().build();
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
}
