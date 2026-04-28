package gg.agit.konect.domain.event.dto;

public record EventMiniEventSummaryResponse(
    Integer miniEventId,
    String title,
    String thumbnailUrl,
    String description,
    String reward,
    String status,
    boolean joined
) {
}
