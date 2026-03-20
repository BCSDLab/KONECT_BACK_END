package gg.agit.konect.domain.club.dto;

public record SheetImportResponse(
    int importedCount
) {
    public static SheetImportResponse of(int importedCount) {
        return new SheetImportResponse(importedCount);
    }
}
