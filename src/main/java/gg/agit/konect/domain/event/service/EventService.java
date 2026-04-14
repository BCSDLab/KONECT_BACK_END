package gg.agit.konect.domain.event.service;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_EVENT;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.event.dto.EventBoothMapResponse;
import gg.agit.konect.domain.event.dto.EventBoothSummaryResponse;
import gg.agit.konect.domain.event.dto.EventBoothsResponse;
import gg.agit.konect.domain.event.dto.EventMiniEventSummaryResponse;
import gg.agit.konect.domain.event.dto.EventMiniEventsResponse;
import gg.agit.konect.domain.event.dto.EventProgramSummaryResponse;
import gg.agit.konect.domain.event.dto.EventProgramsResponse;
import gg.agit.konect.domain.event.enums.EventProgramType;
import gg.agit.konect.domain.event.model.EventBooth;
import gg.agit.konect.domain.event.model.EventBoothMap;
import gg.agit.konect.domain.event.model.EventBoothMapItem;
import gg.agit.konect.domain.event.model.EventMiniEvent;
import gg.agit.konect.domain.event.model.EventProgram;
import gg.agit.konect.domain.event.repository.EventBoothMapItemRepository;
import gg.agit.konect.domain.event.repository.EventBoothMapRepository;
import gg.agit.konect.domain.event.repository.EventBoothRepository;
import gg.agit.konect.domain.event.repository.EventMiniEventRepository;
import gg.agit.konect.domain.event.repository.EventProgramRepository;
import gg.agit.konect.domain.event.repository.EventRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final EventProgramRepository eventProgramRepository;
    private final EventBoothRepository eventBoothRepository;
    private final EventBoothMapRepository eventBoothMapRepository;
    private final EventBoothMapItemRepository eventBoothMapItemRepository;
    private final EventMiniEventRepository eventMiniEventRepository;

    public EventProgramsResponse getEventPrograms(Integer eventId, EventProgramType type, Integer page, Integer limit,
        Integer userId) {
        getEvent(eventId);

        List<EventProgram> filteredPrograms = eventProgramRepository.findAllByEventIdOrderByDisplayOrderAscIdAsc(
                eventId).stream()
            .filter(program -> type == EventProgramType.ALL || program.getType() == type)
            .toList();

        PagedResult<EventProgram> pagedPrograms = paginate(filteredPrograms, page, limit);
        List<EventProgramSummaryResponse> programs = pagedPrograms.items().stream()
            .map(this::toEventProgramSummaryResponse)
            .toList();

        return new EventProgramsResponse(
            (long)pagedPrograms.totalCount(),
            programs.size(),
            pagedPrograms.totalPage(),
            page,
            type,
            programs
        );
    }

    public EventBoothsResponse getEventBooths(Integer eventId, String category, String keyword, Integer page,
        Integer limit) {
        getEvent(eventId);

        List<EventBooth> filteredBooths = eventBoothRepository.findAllByEventIdOrderByDisplayOrderAscIdAsc(eventId)
            .stream()
            .filter(booth -> category == null || category.isBlank() || booth.getCategory().equalsIgnoreCase(category))
            .filter(booth -> keyword == null || keyword.isBlank() || booth.getName().contains(keyword))
            .toList();

        PagedResult<EventBooth> pagedBooths = paginate(filteredBooths, page, limit);
        List<EventBoothSummaryResponse> booths = pagedBooths.items().stream()
            .map(this::toEventBoothSummaryResponse)
            .toList();

        return new EventBoothsResponse(
            (long)pagedBooths.totalCount(),
            booths.size(),
            pagedBooths.totalPage(),
            page,
            booths
        );
    }

    public EventBoothMapResponse getEventBoothMap(Integer eventId) {
        EventBoothMap boothMap = eventBoothMapRepository.findByEventId(eventId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_EVENT));

        List<EventBoothMapItem> boothMapItems = eventBoothMapItemRepository.findAllByEventBoothMapIdOrderByIdAsc(
            boothMap.getId());
        List<EventBoothMapResponse.BoothMapItemResponse> booths = boothMapItems.stream()
            .map(this::toEventBoothMapItemResponse)
            .toList();

        List<EventBoothMapResponse.ZoneResponse> zones = booths.stream()
            .map(EventBoothMapResponse.BoothMapItemResponse::zone)
            .filter(zone -> zone != null && !zone.isBlank())
            .distinct()
            .map(zone -> new EventBoothMapResponse.ZoneResponse(zone, zone))
            .toList();

        return new EventBoothMapResponse(
            boothMap.getMapImageUrl(),
            zones,
            booths
        );
    }

    public EventMiniEventsResponse getEventMiniEvents(Integer eventId, Integer page, Integer limit, Integer userId) {
        getEvent(eventId);

        List<EventMiniEvent> miniEvents = eventMiniEventRepository.findAllByEventIdOrderByDisplayOrderAscIdAsc(eventId);
        PagedResult<EventMiniEvent> pagedMiniEvents = paginate(miniEvents, page, limit);
        List<EventMiniEventSummaryResponse> miniEventResponses = pagedMiniEvents.items().stream()
            .map(this::toEventMiniEventSummaryResponse)
            .toList();

        return new EventMiniEventsResponse(
            (long)pagedMiniEvents.totalCount(),
            miniEventResponses.size(),
            pagedMiniEvents.totalPage(),
            page,
            miniEventResponses
        );
    }

    private void getEvent(Integer eventId) {
        eventRepository.findById(eventId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_EVENT));
    }

    private <T> PagedResult<T> paginate(List<T> items, Integer page, Integer limit) {
        int totalCount = items.size();
        int fromIndex = Math.max((page - 1) * limit, 0);
        int toIndex = Math.min(fromIndex + limit, totalCount);
        List<T> pagedItems = fromIndex >= totalCount ? List.of() : items.subList(fromIndex, toIndex);
        int totalPage = totalCount == 0 ? 0 : (int)Math.ceil((double)totalCount / limit);
        return new PagedResult<>(pagedItems, totalCount, totalPage);
    }

    private EventProgramSummaryResponse toEventProgramSummaryResponse(EventProgram program) {
        return new EventProgramSummaryResponse(
            program.getId(),
            program.getTitle(),
            program.getDescription(),
            program.getThumbnailUrl(),
            program.getRewardPoint(),
            program.getStatus().name(),
            false
        );
    }

    private EventBoothSummaryResponse toEventBoothSummaryResponse(EventBooth booth) {
        return new EventBoothSummaryResponse(
            booth.getId(),
            booth.getName(),
            booth.getCategory(),
            booth.getLocationLabel(),
            booth.getZone(),
            booth.getThumbnailUrl(),
            Boolean.TRUE.equals(booth.getIsOpen())
        );
    }

    private EventBoothMapResponse.BoothMapItemResponse toEventBoothMapItemResponse(EventBoothMapItem boothMapItem) {
        EventBooth booth = boothMapItem.getEventBooth();

        return new EventBoothMapResponse.BoothMapItemResponse(
            booth.getId(),
            booth.getName(),
            booth.getZone(),
            boothMapItem.getX(),
            boothMapItem.getY(),
            boothMapItem.getWidth(),
            boothMapItem.getHeight(),
            boothMapItem.getStatus().name()
        );
    }

    private EventMiniEventSummaryResponse toEventMiniEventSummaryResponse(EventMiniEvent miniEvent) {
        return new EventMiniEventSummaryResponse(
            miniEvent.getId(),
            miniEvent.getTitle(),
            miniEvent.getThumbnailUrl(),
            miniEvent.getDescription(),
            miniEvent.getRewardLabel(),
            miniEvent.getStatus().name(),
            false
        );
    }

    private record PagedResult<T>(List<T> items, int totalCount, int totalPage) {
    }
}
