package gg.agit.konect.council.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.council.dto.CouncilCreateRequest;
import gg.agit.konect.council.dto.CouncilResponse;
import gg.agit.konect.council.dto.CouncilUpdateRequest;
import gg.agit.konect.council.service.CouncilService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/councils")
public class CouncilController implements CouncilApi {

    private final CouncilService councilService;

    @GetMapping
    public ResponseEntity<CouncilResponse> getCouncil() {
        CouncilResponse response = councilService.getCouncil();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CouncilResponse> createCouncil(
        @Valid @RequestBody CouncilCreateRequest request
    ) {
        CouncilResponse response = councilService.createCouncil(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<CouncilResponse> updateCouncil(
        @Valid @RequestBody CouncilUpdateRequest request
    ) {
        CouncilResponse response = councilService.updateCouncil(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteCouncil() {
        councilService.deleteCouncil();
        return ResponseEntity.noContent().build();
    }
}
