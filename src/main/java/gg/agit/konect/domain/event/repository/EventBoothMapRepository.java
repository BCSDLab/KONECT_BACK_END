package gg.agit.konect.domain.event.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.event.model.EventBoothMap;

public interface EventBoothMapRepository extends Repository<EventBoothMap, Integer> {

    Optional<EventBoothMap> findByEventId(Integer eventId);
}
