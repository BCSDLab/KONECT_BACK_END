package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.club.dto.ClubRegistrationRequestDto;
import gg.agit.konect.domain.club.service.ClubRegistrationRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Club Registration", description = "동아리 등록 요청 API")
@RestController
@RequestMapping("/clubs")
@RequiredArgsConstructor
public class ClubRegistrationRequestController implements ClubRegistrationRequestApi {

    private final ClubRegistrationRequestService clubRegistrationRequestService;

    @Override
    @Operation(summary = "동아리 등록 요청", description = "비로그인 사용자가 새 동아리 등록을 요청합니다.")
    @PostMapping("/registration-requests")
    public ResponseEntity<Void> registerClub(
        @Valid @RequestBody ClubRegistrationRequestDto request
    ) {
        clubRegistrationRequestService.register(request);
        return ResponseEntity.ok().build();
    }
}
