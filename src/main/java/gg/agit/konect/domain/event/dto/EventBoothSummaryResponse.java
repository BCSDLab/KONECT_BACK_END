package gg.agit.konect.domain.event.dto;

public record EventBoothSummaryResponse(
    Integer boothId,
    String name,
    String category,
    String locationLabel,
    String zone,
    String thumbnailUrl,
    boolean open
) {
}
