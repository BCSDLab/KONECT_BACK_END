package gg.agit.konect.domain.event.dto;

import java.util.List;

public record EventContentsResponse(
    Long totalCount,
    Integer currentCount,
    Integer totalPage,
    Integer currentPage,
    List<EventContentSummaryResponse> contents
) {
}
