package gg.agit.konect.domain.bank.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.bank.model.Bank;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface BankRepository extends Repository<Bank, Integer> {

    List<Bank> findAll();

    Optional<Bank> findById(Integer id);

    Optional<Bank> findByName(String name);

    default Bank getById(Integer id) {
        return findById(id).orElseThrow(() ->
            CustomException.of(ApiResponseCode.NOT_FOUND_BANK));
    }

    default Bank getByName(String name) {
        return findByName(name).orElseThrow(() ->
            CustomException.of(ApiResponseCode.NOT_FOUND_BANK));
    }
}
