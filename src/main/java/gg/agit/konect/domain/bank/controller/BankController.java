package gg.agit.konect.domain.bank.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.bank.dto.BanksResponse;
import gg.agit.konect.domain.bank.service.BankService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/banks")
public class BankController implements BankApi {

    private final BankService bankService;

    @Override
    public ResponseEntity<BanksResponse> getBanks() {
        BanksResponse response = bankService.getBanks();
        return ResponseEntity.ok(response);
    }
}
