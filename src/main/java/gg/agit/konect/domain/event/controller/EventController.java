package gg.agit.konect.domain.event.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.event.dto.EventBoothMapResponse;
import gg.agit.konect.domain.event.dto.EventBoothsResponse;
import gg.agit.konect.domain.event.dto.EventMiniEventsResponse;
import gg.agit.konect.domain.event.dto.EventProgramsResponse;
import gg.agit.konect.domain.event.enums.EventProgramType;
import gg.agit.konect.domain.event.service.EventService;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
public class EventController implements EventApi {

    private final EventService eventService;

    @Override
    public ResponseEntity<EventProgramsResponse> getEventPrograms(Integer eventId, EventProgramType type, Integer page,
        Integer limit,
        Integer userId) {
        return ResponseEntity.ok(eventService.getEventPrograms(eventId, type, page, limit, userId));
    }

    @Override
    public ResponseEntity<EventBoothsResponse> getEventBooths(Integer eventId, String category, String keyword,
        Integer page, Integer limit) {
        return ResponseEntity.ok(eventService.getEventBooths(eventId, category, keyword, page, limit));
    }

    @Override
    public ResponseEntity<EventBoothMapResponse> getEventBoothMap(Integer eventId) {
        return ResponseEntity.ok(eventService.getEventBoothMap(eventId));
    }

    @Override
    public ResponseEntity<EventMiniEventsResponse> getEventMiniEvents(Integer eventId, Integer page, Integer limit,
        Integer userId) {
        return ResponseEntity.ok(eventService.getEventMiniEvents(eventId, page, limit, userId));
    }
}
