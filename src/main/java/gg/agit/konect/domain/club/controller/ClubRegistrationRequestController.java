package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.club.dto.ClubRegistrationRequest;
import gg.agit.konect.domain.club.service.ClubRegistrationRequestService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs/registration-requests")
public class ClubRegistrationRequestController implements ClubRegistrationRequestApi {

    private final ClubRegistrationRequestService clubRegistrationRequestService;

    @Override
    public ResponseEntity<Void> submitClubRegistrationRequest(ClubRegistrationRequest request) {
        clubRegistrationRequestService.submitClubRegistrationRequest(request);
        return ResponseEntity.ok().build();
    }
}
