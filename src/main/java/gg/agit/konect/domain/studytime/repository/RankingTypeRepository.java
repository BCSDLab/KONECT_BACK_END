package gg.agit.konect.domain.studytime.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.studytime.model.RankingType;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface RankingTypeRepository extends Repository<RankingType, Integer> {

    Optional<RankingType> findByNameIgnoreCase(String name);

    default RankingType getByNameIgnoreCase(String name) {
        return findByNameIgnoreCase(name)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_RANKING_TYPE));
    }
}
