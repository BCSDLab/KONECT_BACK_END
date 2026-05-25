package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;

import gg.agit.konect.domain.club.dto.ClubRegistrationRequestDto;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

public interface ClubRegistrationRequestApi {

    ResponseEntity<Void> registerClub(
        @RequestBody ClubRegistrationRequestDto request
    );
}
