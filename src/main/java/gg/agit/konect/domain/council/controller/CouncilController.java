package gg.agit.konect.domain.council.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.council.dto.CouncilCreateRequest;
import gg.agit.konect.domain.council.dto.CouncilResponse;
import gg.agit.konect.domain.council.dto.CouncilUpdateRequest;
import gg.agit.konect.domain.council.service.CouncilService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/councils")
public class CouncilController implements CouncilApi {

    private final CouncilService councilService;

    @GetMapping
    public ResponseEntity<CouncilResponse> getCouncil(@UserId Integer userId) {
        CouncilResponse response = councilService.getCouncil(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Void> createCouncil(
        @UserId Integer userId,
        @Valid @RequestBody CouncilCreateRequest request
    ) {
        councilService.createCouncil(userId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping
    public ResponseEntity<Void> updateCouncil(
        @UserId Integer userId,
        @Valid @RequestBody CouncilUpdateRequest request
    ) {
        councilService.updateCouncil(userId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteCouncil(@UserId Integer userId) {
        councilService.deleteCouncil(userId);
        return ResponseEntity.noContent().build();
    }
}
