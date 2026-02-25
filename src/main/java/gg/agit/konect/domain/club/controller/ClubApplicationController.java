package gg.agit.konect.domain.club.controller;

import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.club.dto.ClubApplicationAnswersResponse;
import gg.agit.konect.domain.club.dto.ClubApplicationCondition;
import gg.agit.konect.domain.club.dto.ClubApplicationsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyRequest;
import gg.agit.konect.domain.club.dto.ClubFeeInfoReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubFeeInfoResponse;
import gg.agit.konect.domain.club.enums.ClubApplicationSortBy;
import gg.agit.konect.domain.club.service.ClubApplicationService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
@Validated
public class ClubApplicationController implements ClubApplicationApi {

    private final ClubApplicationService clubApplicationService;

    @Override
    public ResponseEntity<ClubFeeInfoResponse> applyClub(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubApplyRequest request,
        @UserId Integer userId
    ) {
        ClubFeeInfoResponse response = clubApplicationService.applyClub(clubId, userId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubApplicationsResponse> getClubApplications(
        @PathVariable(name = "clubId") Integer clubId,
        @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
        @RequestParam(defaultValue = "1") Integer page,
        @Min(value = 1, message = "페이지 당 항목 수는 1 이상이어야 합니다.")
        @RequestParam(defaultValue = "10") Integer limit,
        @RequestParam(defaultValue = "APPLIED_AT") ClubApplicationSortBy sortBy,
        @RequestParam(defaultValue = "ASC") Sort.Direction sortDirection,
        @UserId Integer userId
    ) {
        ClubApplicationCondition condition = new ClubApplicationCondition(page, limit, sortBy, sortDirection);
        ClubApplicationsResponse response = clubApplicationService.getClubApplications(clubId, userId, condition);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubApplicationAnswersResponse> getClubApplicationAnswers(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "applicationId") Integer applicationId,
        @UserId Integer userId
    ) {
        ClubApplicationAnswersResponse response = clubApplicationService.getClubApplicationAnswers(
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
        clubApplicationService.approveClubApplication(clubId, applicationId, userId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> rejectClubApplication(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "applicationId") Integer applicationId,
        @UserId Integer userId
    ) {
        clubApplicationService.rejectClubApplication(clubId, applicationId, userId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<ClubApplyQuestionsResponse> getApplyQuestions(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    ) {
        ClubApplyQuestionsResponse response = clubApplicationService.getApplyQuestions(clubId, userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubApplyQuestionsResponse> replaceApplyQuestions(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubApplyQuestionsReplaceRequest request,
        @UserId Integer userId
    ) {
        ClubApplyQuestionsResponse response = clubApplicationService.replaceApplyQuestions(clubId, userId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubFeeInfoResponse> getFeeInfo(
        @PathVariable(name = "clubId") Integer clubId
    ) {
        ClubFeeInfoResponse response = clubApplicationService.getFeeInfo(clubId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubFeeInfoResponse> replaceFeeInfo(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubFeeInfoReplaceRequest request,
        @UserId Integer userId
    ) {
        ClubFeeInfoResponse response = clubApplicationService.replaceFeeInfo(clubId, userId, request);
        return ResponseEntity.ok(response);
    }
}
