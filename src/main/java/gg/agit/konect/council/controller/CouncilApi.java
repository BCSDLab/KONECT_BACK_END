package gg.agit.konect.council.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.council.dto.CouncilResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "(Normal) Council: 총동아리연합회", description = "총동아리연합회 API")
@RequestMapping("/councils")
public interface CouncilApi {

    @Operation(summary = "총동아리연합회 정보를 조회한다.")
    @GetMapping
    ResponseEntity<CouncilResponse> getCouncil();
}
