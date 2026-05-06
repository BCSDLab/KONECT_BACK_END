package gg.agit.konect.domain.event.dto;

import java.time.LocalDateTime;

public record EventHomeResponse(
    Integer eventId,
    String title,
    String subtitle,
    String posterImageUrl,
    LocalDateTime startAt,
    LocalDateTime endAt,
    String notice,
    Summary summary,
    UserStatus userStatus
) {

    public record Summary(
        Integer programCount,
        Integer boothCount,
        Integer eventCount,
        Integer contentCount
    ) {
    }

    public record UserStatus(
        Integer point,
        Integer participatedEventCount
    ) {
    }
}
