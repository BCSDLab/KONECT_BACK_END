package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.club.dto.ClubSettingsResponse;
import gg.agit.konect.domain.club.dto.ClubSettingsUpdateRequest;
import gg.agit.konect.domain.club.service.ClubSettingsService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubSettingsController implements ClubSettingsApi {

    private final ClubSettingsService clubSettingsService;

    @Override
    public ResponseEntity<ClubSettingsResponse> getSettings(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    ) {
        ClubSettingsResponse response = clubSettingsService.getSettings(clubId, userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubSettingsResponse> updateSettings(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubSettingsUpdateRequest request,
        @UserId Integer userId
    ) {
        ClubSettingsResponse response = clubSettingsService.updateSettings(clubId, userId, request);
        return ResponseEntity.ok(response);
    }
}
