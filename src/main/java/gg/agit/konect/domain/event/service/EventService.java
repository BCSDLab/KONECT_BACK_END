package gg.agit.konect.domain.event.service;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_EVENT;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.event.dto.EventBoothMapResponse;
import gg.agit.konect.domain.event.dto.EventBoothSummaryResponse;
import gg.agit.konect.domain.event.dto.EventBoothsResponse;
import gg.agit.konect.domain.event.dto.EventContentSummaryResponse;
import gg.agit.konect.domain.event.dto.EventContentsResponse;
import gg.agit.konect.domain.event.dto.EventHomeResponse;
import gg.agit.konect.domain.event.dto.EventMiniEventSummaryResponse;
import gg.agit.konect.domain.event.dto.EventMiniEventsResponse;
import gg.agit.konect.domain.event.dto.EventProgramSummaryResponse;
import gg.agit.konect.domain.event.dto.EventProgramsResponse;
import gg.agit.konect.domain.event.enums.EventProgramType;
import gg.agit.konect.domain.event.model.Event;
import gg.agit.konect.domain.event.model.EventBooth;
import gg.agit.konect.domain.event.model.EventBoothMap;
import gg.agit.konect.domain.event.model.EventBoothMapItem;
import gg.agit.konect.domain.event.model.EventContent;
import gg.agit.konect.domain.event.model.EventMiniEvent;
import gg.agit.konect.domain.event.model.EventProgram;
import gg.agit.konect.domain.event.repository.EventBoothMapItemRepository;
import gg.agit.konect.domain.event.repository.EventBoothMapRepository;
import gg.agit.konect.domain.event.repository.EventBoothRepository;
import gg.agit.konect.domain.event.repository.EventContentRepository;
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
    private final EventContentRepository eventContentRepository;

    public EventHomeResponse getEventHome(Integer eventId, Integer userId) {
        Event event = getEvent(eventId);

        return new EventHomeResponse(
            event.getId(),
            event.getTitle(),
            event.getSubtitle(),
            event.getPosterImageUrl(),
            event.getStartAt(),
            event.getEndAt(),
            event.getNotice(),
            new EventHomeResponse.Summary(
                eventProgramRepository.countByEventId(eventId),
                eventBoothRepository.countByEventId(eventId),
                eventMiniEventRepository.countByEventId(eventId),
                eventContentRepository.countByEventId(eventId)
            ),
            new EventHomeResponse.UserStatus(0, 0)
        );
    }

    public EventProgramsResponse getEventPrograms(Integer eventId, EventProgramType type, Integer page, Integer limit, Integer userId) {
        getEvent(eventId);

        List<EventProgram> filteredPrograms = eventProgramRepository.findAllByEventIdOrderByDisplayOrderAscIdAsc(eventId).stream()
            .filter(program -> type == EventProgramType.ALL || program.getType() == type)
            .toList();

        int fromIndex = Math.max((page - 1) * limit, 0);
        int toIndex = Math.min(fromIndex + limit, filteredPrograms.size());
        List<EventProgramSummaryResponse> programs = fromIndex >= filteredPrograms.size()
            ? List.of()
            : filteredPrograms.subList(fromIndex, toIndex).stream()
                .map(this::toEventProgramSummaryResponse)
                .toList();

        int totalCount = filteredPrograms.size();
        int totalPage = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / limit);

        return new EventProgramsResponse(
            (long) totalCount,
            programs.size(),
            totalPage,
            page,
            type,
            programs
        );
    }

    public EventBoothsResponse getEventBooths(Integer eventId, String category, String keyword, Integer page, Integer limit) {
        getEvent(eventId);

        List<EventBooth> filteredBooths = eventBoothRepository.findAllByEventIdOrderByDisplayOrderAscIdAsc(eventId).stream()
            .filter(booth -> category == null || category.isBlank() || booth.getCategory().equalsIgnoreCase(category))
            .filter(booth -> keyword == null || keyword.isBlank() || booth.getName().contains(keyword))
            .toList();

        int fromIndex = Math.max((page - 1) * limit, 0);
        int toIndex = Math.min(fromIndex + limit, filteredBooths.size());
        List<EventBoothSummaryResponse> booths = fromIndex >= filteredBooths.size()
            ? List.of()
            : filteredBooths.subList(fromIndex, toIndex).stream()
                .map(this::toEventBoothSummaryResponse)
                .toList();

        int totalCount = filteredBooths.size();
        int totalPage = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / limit);

        return new EventBoothsResponse(
            (long) totalCount,
            booths.size(),
            totalPage,
            page,
            booths
        );
    }

    public EventBoothMapResponse getEventBoothMap(Integer eventId) {
        EventBoothMap boothMap = eventBoothMapRepository.findByEventId(eventId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_EVENT));

        List<EventBoothMapItem> boothMapItems = eventBoothMapItemRepository.findAllByEventBoothMapIdOrderByIdAsc(boothMap.getId());
        List<EventBoothMapResponse.BoothMapItemResponse> booths = boothMapItems.stream()
            .map(this::toEventBoothMapItemResponse)
            .toList();

        // 부스 목록 응답과 같은 구역 값을 맵에서도 그대로 내려 프론트가 별도 매핑 없이 재사용할 수 있게 맞춘다.
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
        int fromIndex = Math.max((page - 1) * limit, 0);
        int toIndex = Math.min(fromIndex + limit, miniEvents.size());
        List<EventMiniEventSummaryResponse> miniEventResponses = fromIndex >= miniEvents.size()
            ? List.of()
            : miniEvents.subList(fromIndex, toIndex).stream()
                .map(this::toEventMiniEventSummaryResponse)
                .toList();

        int totalCount = miniEvents.size();
        int totalPage = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / limit);

        return new EventMiniEventsResponse(
            (long) totalCount,
            miniEventResponses.size(),
            totalPage,
            page,
            miniEventResponses
        );
    }

    public EventContentsResponse getEventContents(Integer eventId, String category, Integer page, Integer limit) {
        getEvent(eventId);

        List<EventContent> filteredContents = eventContentRepository.findAllByEventIdOrderByDisplayOrderAscIdAsc(eventId).stream()
            // 콘텐츠 타입 enum 이름과 동일한 문자열만 허용해 의도하지 않은 부분 일치를 막는다.
            .filter(content -> category == null || category.isBlank() || content.getType().name().equalsIgnoreCase(category))
            .toList();

        int fromIndex = Math.max((page - 1) * limit, 0);
        int toIndex = Math.min(fromIndex + limit, filteredContents.size());
        List<EventContentSummaryResponse> contents = fromIndex >= filteredContents.size()
            ? List.of()
            : filteredContents.subList(fromIndex, toIndex).stream()
                .map(this::toEventContentSummaryResponse)
                .toList();

        int totalCount = filteredContents.size();
        int totalPage = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / limit);

        return new EventContentsResponse(
            (long) totalCount,
            contents.size(),
            totalPage,
            page,
            contents
        );
    }

    private Event getEvent(Integer eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_EVENT));
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

    private EventContentSummaryResponse toEventContentSummaryResponse(EventContent content) {
        return new EventContentSummaryResponse(
            content.getId(),
            content.getTitle(),
            content.getThumbnailUrl(),
            content.getType().name(),
            content.getSummary(),
            content.getPublishedAt()
        );
    }
}
