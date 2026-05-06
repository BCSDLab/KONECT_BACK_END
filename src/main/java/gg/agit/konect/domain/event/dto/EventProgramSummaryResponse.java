package gg.agit.konect.domain.event.dto;

public record EventProgramSummaryResponse(
    Integer programId,
    String title,
    String description,
    String thumbnailUrl,
    Integer rewardPoint,
    String status,
    boolean participated
) {
}
