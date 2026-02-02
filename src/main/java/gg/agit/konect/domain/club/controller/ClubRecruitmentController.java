package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.club.dto.ClubRecruitmentResponse;
import gg.agit.konect.domain.club.dto.ClubRecruitmentUpsertRequest;
import gg.agit.konect.domain.club.service.ClubRecruitmentService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubRecruitmentController implements ClubRecruitmentApi {

    private final ClubRecruitmentService clubRecruitmentService;

    @Override
    public ResponseEntity<ClubRecruitmentResponse> getRecruitments(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    ) {
        ClubRecruitmentResponse response = clubRecruitmentService.getRecruitment(clubId, userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> upsertRecruitment(
        @Valid @RequestBody ClubRecruitmentUpsertRequest request,
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    ) {
        clubRecruitmentService.upsertRecruitment(clubId, userId, request);
        return ResponseEntity.noContent().build();
    }
}
