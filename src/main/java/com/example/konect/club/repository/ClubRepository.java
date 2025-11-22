package com.example.konect.club.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import com.example.konect.club.model.Club;

public interface ClubRepository extends Repository<Club, Integer> {

    Page<Club> findAll(Pageable pageable);
}
