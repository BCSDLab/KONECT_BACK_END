package gg.agit.konect.domain.event.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.event.model.Event;

public interface EventRepository extends Repository<Event, Integer> {

    Optional<Event> findById(Integer id);

    Event save(Event event);
}
