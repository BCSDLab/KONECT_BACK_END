package gg.agit.konect.domain.event.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.event.model.EventMiniEvent;

public interface EventMiniEventRepository extends Repository<EventMiniEvent, Integer> {

    List<EventMiniEvent> findAllByEventIdOrderByDisplayOrderAscIdAsc(Integer eventId);

    int countByEventId(Integer eventId);
}
