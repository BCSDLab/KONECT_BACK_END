package gg.agit.konect.domain.bank.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.bank.dto.BanksResponse;
import gg.agit.konect.domain.bank.model.Bank;
import gg.agit.konect.domain.bank.repository.BankRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private final BankRepository bankRepository;

    public BanksResponse getBanks() {
        List<Bank> banks = bankRepository.findAll();
        return BanksResponse.from(banks);
    }
}
