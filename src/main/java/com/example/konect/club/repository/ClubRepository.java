package com.example.konect.club.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.example.konect.club.model.Club;
import com.example.konect.global.code.ApiResponseCode;
import com.example.konect.global.exception.CustomException;

public interface ClubRepository extends Repository<Club, Integer> {

    Optional<Club> findById(Integer id);

    default Club getById(Integer id) {
        return findById(id).orElseThrow(() ->
            CustomException.of(ApiResponseCode.NOT_FOUND_CLUB));
    }
}
