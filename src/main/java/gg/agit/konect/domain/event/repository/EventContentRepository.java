package gg.agit.konect.domain.event.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.event.model.EventContent;

public interface EventContentRepository extends Repository<EventContent, Integer> {

    List<EventContent> findAllByEventIdOrderByDisplayOrderAscIdAsc(Integer eventId);

    int countByEventId(Integer eventId);
}
