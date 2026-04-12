package gg.agit.konect.domain.event.service;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_EVENT;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.event.dto.EventBoothMapResponse;
import gg.agit.konect.domain.event.dto.EventBoothSummaryResponse;
import gg.agit.konect.domain.event.dto.EventBoothsResponse;
import gg.agit.konect.domain.event.dto.EventContentsResponse;
import gg.agit.konect.domain.event.dto.EventHomeResponse;
import gg.agit.konect.domain.event.dto.EventMiniEventsResponse;
import gg.agit.konect.domain.event.dto.EventProgramSummaryResponse;
import gg.agit.konect.domain.event.dto.EventProgramsResponse;
import gg.agit.konect.domain.event.enums.EventProgramType;
import gg.agit.konect.domain.event.model.Event;
import gg.agit.konect.domain.event.model.EventBooth;
import gg.agit.konect.domain.event.model.EventProgram;
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
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public EventMiniEventsResponse getEventMiniEvents(Integer eventId, Integer page, Integer limit, Integer userId) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public EventContentsResponse getEventContents(Integer eventId, String category, Integer page, Integer limit) {
        throw new UnsupportedOperationException("Not implemented yet.");
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
}
