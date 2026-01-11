package gg.agit.konect.domain.bank.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.bank.dto.BanksResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "(Normal) Bank: 은행", description = "은행 API")
@RequestMapping("/banks")
public interface BankApi {

    @Operation(summary = "은행 리스트를 조회한다.")
    @GetMapping
    ResponseEntity<BanksResponse> getBanks();
}
