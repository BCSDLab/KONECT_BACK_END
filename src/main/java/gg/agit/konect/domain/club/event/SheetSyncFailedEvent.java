package gg.agit.konect.domain.club.event;

import java.time.LocalDateTime;

public record SheetSyncFailedEvent(
    Integer clubId,
    String spreadsheetId,
    boolean accessDenied,
    String reason,
    LocalDateTime occurredAt
) {

    public static SheetSyncFailedEvent accessDenied(
        Integer clubId,
        String spreadsheetId,
        String reason
    ) {
        return new SheetSyncFailedEvent(clubId, spreadsheetId, true, reason, LocalDateTime.now());
    }

    public static SheetSyncFailedEvent unexpected(
        Integer clubId,
        String spreadsheetId,
        String reason
    ) {
        return new SheetSyncFailedEvent(clubId, spreadsheetId, false, reason, LocalDateTime.now());
    }
}
