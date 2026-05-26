package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import gg.agit.konect.domain.club.dto.ClubInformationUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubRegistrationRequestDto;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

public interface ClubRegistrationRequestApi {

    ResponseEntity<Void> registerClub(
        @RequestBody ClubRegistrationRequestDto request
    );

    ResponseEntity<Void> requestClubInformationUpdate(
        @PathVariable(name = "clubId") Integer clubId,
        @RequestBody ClubInformationUpdateRequest request
    );
}
