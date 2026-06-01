package gg.agit.konect.domain.website.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.website.model.WebClub;

public interface WebClubRepository extends Repository<WebClub, Integer> {

    @Query("""
        SELECT c.name
        FROM WebClub c
        WHERE c.university.id = :universityId
        AND c.name IN :names
        """)
    Set<String> findExistingNamesByUniversityId(
        @Param("universityId") Integer universityId,
        @Param("names") Set<String> names
    );

    List<WebClub> saveAll(Iterable<WebClub> clubs);
}
