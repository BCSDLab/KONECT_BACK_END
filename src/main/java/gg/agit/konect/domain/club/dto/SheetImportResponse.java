package gg.agit.konect.domain.club.dto;

import java.util.List;

public record SheetImportResponse(
    int importedCount,
    int autoRegisteredCount,
    List<String> warnings
) {

    public static SheetImportResponse of(int importedCount) {
        return new SheetImportResponse(importedCount, 0, List.of());
    }

    public static SheetImportResponse of(
        int importedCount,
        int autoRegisteredCount,
        List<String> warnings
    ) {
        return new SheetImportResponse(
            importedCount,
            autoRegisteredCount,
            warnings != null ? warnings : List.of()
        );
    }
}
