package gg.agit.konect.domain.event.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.event.model.EventBoothMapItem;

public interface EventBoothMapItemRepository extends Repository<EventBoothMapItem, Integer> {

    List<EventBoothMapItem> findAllByEventBoothMapIdOrderByIdAsc(Integer eventBoothMapId);
}
