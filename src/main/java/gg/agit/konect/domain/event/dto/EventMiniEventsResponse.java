package gg.agit.konect.domain.event.dto;

import java.util.List;

public record EventMiniEventsResponse(
    Long totalCount,
    Integer currentCount,
    Integer totalPage,
    Integer currentPage,
    List<EventMiniEventSummaryResponse> miniEvents
) {
}
