package gg.agit.konect.domain.event.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.event.model.EventBooth;

public interface EventBoothRepository extends Repository<EventBooth, Integer> {

    List<EventBooth> findAllByEventIdOrderByDisplayOrderAscIdAsc(Integer eventId);

    int countByEventId(Integer eventId);
}
