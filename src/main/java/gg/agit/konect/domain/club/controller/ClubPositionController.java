package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.club.dto.ClubPositionCreateRequest;
import gg.agit.konect.domain.club.dto.ClubPositionUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubPositionsResponse;
import gg.agit.konect.domain.club.service.ClubPositionService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubPositionController implements ClubPositionApi {

    private final ClubPositionService clubPositionService;

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
}
