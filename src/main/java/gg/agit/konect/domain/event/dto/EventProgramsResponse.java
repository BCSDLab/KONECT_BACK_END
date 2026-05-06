package gg.agit.konect.domain.event.dto;

import java.util.List;

import gg.agit.konect.domain.event.enums.EventProgramType;

public record EventProgramsResponse(
    Long totalCount,
    Integer currentCount,
    Integer totalPage,
    Integer currentPage,
    EventProgramType type,
    List<EventProgramSummaryResponse> programs
) {
}
