package gg.agit.konect.domain.event.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.event.model.EventProgram;

public interface EventProgramRepository extends Repository<EventProgram, Integer> {

    List<EventProgram> findAllByEventIdOrderByDisplayOrderAscIdAsc(Integer eventId);

    int countByEventId(Integer eventId);
}
