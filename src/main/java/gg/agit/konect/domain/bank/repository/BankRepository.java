package gg.agit.konect.domain.bank.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.bank.model.Bank;

public interface BankRepository extends Repository<Bank, Integer> {

    List<Bank> findAll();
}
