package com.example.konect.club.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.example.konect.club.model.Club;

public interface ClubRepository extends Repository<Club, Integer> {

    Optional<Club> findById(Integer id);

    default Club getById(Integer id) {
        return findById(id).orElseThrow(() ->
            new RuntimeException("동아리를 찾을 수 없습니다."));
    }
}
