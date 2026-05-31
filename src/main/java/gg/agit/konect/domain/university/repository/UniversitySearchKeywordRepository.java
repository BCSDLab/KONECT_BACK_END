package gg.agit.konect.domain.university.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.university.model.UniversitySearchKeyword;

public interface UniversitySearchKeywordRepository extends Repository<UniversitySearchKeyword, Integer> {

    @Query("""
        SELECT keyword
        FROM UniversitySearchKeyword keyword
        JOIN FETCH keyword.university university
        WHERE university.koreanName IN :universityNames
        """)
    List<UniversitySearchKeyword> findAllByUniversityNames(
        @Param("universityNames") Collection<String> universityNames
    );
}
