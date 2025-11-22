package com.example.konect.club.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.example.konect.club.model.ClubTagMap;
import com.example.konect.club.model.ClubTagMapId;

public interface ClubTagMapRepository extends Repository<ClubTagMap, ClubTagMapId> {

    @Query(value = """
        SELECT * FROM club_tag_map
        WHERE club_id IN :clubIds
        """, nativeQuery = true)
    List<ClubTagMap> findByClubIdIn(@Param(value = "clubIds") List<Integer> clubIds);
}
