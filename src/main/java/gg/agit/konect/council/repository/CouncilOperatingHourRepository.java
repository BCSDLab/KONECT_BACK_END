package gg.agit.konect.council.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.council.model.CouncilOperatingHour;

public interface CouncilOperatingHourRepository extends Repository<CouncilOperatingHour, Integer> {

    List<CouncilOperatingHour> findByCouncilId(Integer councilId);

    void deleteByCouncilId(Integer councilId);

    void save(CouncilOperatingHour councilOperatingHour);
}
