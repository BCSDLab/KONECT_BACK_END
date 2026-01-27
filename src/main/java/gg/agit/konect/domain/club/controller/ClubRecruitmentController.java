package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.club.dto.ClubRecruitmentCreateRequest;
import gg.agit.konect.domain.club.dto.ClubRecruitmentResponse;
import gg.agit.konect.domain.club.dto.ClubRecruitmentUpdateRequest;
import gg.agit.konect.domain.club.service.ClubService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubRecruitmentController implements ClubRecruitmentApi {

    private final ClubService clubService;

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
}
