package gg.agit.konect.domain.studytime.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.studytime.model.RankingType;

public interface RankingTypeRepository extends Repository<RankingType, Integer> {

    Optional<RankingType> findByNameIgnoreCase(String name);
}
