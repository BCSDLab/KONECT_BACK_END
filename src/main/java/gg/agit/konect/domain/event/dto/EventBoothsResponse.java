package gg.agit.konect.domain.event.dto;

import java.util.List;

public record EventBoothsResponse(
    Long totalCount,
    Integer currentCount,
    Integer totalPage,
    Integer currentPage,
    List<EventBoothSummaryResponse> booths
) {
}
