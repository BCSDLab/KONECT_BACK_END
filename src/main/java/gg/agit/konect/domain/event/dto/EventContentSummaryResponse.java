package gg.agit.konect.domain.event.dto;

import java.time.LocalDateTime;

public record EventContentSummaryResponse(
    Integer contentId,
    String title,
    String thumbnailUrl,
    String type,
    String summary,
    LocalDateTime publishedAt
) {
}
