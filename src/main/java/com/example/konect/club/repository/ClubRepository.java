package com.example.konect.club.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.example.konect.club.model.Club;

public interface ClubRepository extends Repository<Club, Integer> {

    Page<Club> findAll(Pageable pageable);

    @Query("""
        SELECT DISTINCT c
        FROM Club c
        LEFT JOIN ClubTagMap ctm
        ON c.id = ctm.club.id
        LEFT JOIN ClubTag ct
        ON ctm.tag.id = ct.id
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(ct.name) LIKE LOWER(CONCAT('%', :query, '%'))
        """)
    Page<Club> findAllByQuery(@Param("query") String query, Pageable pageable);
}
