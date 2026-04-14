package gg.agit.konect.domain.event.service;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_EVENT;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.event.dto.EventProgramSummaryResponse;
import gg.agit.konect.domain.event.dto.EventProgramsResponse;
import gg.agit.konect.domain.event.enums.EventProgramType;
import gg.agit.konect.domain.event.model.EventProgram;
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

    public EventProgramsResponse getEventPrograms(Integer eventId, EventProgramType type, Integer page, Integer limit,
        Integer userId) {
        eventRepository.findById(eventId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_EVENT));

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

    private record PagedResult<T>(List<T> items, int totalCount, int totalPage) {
    }
}
