package gg.agit.konect.admin.website.dto;

import java.util.List;

public record AdminWebsiteClubSheetImportResponse(
    int importedCount,
    int skippedCount,
    List<String> warnings
) {

    public static AdminWebsiteClubSheetImportResponse of(
        int importedCount,
        int skippedCount,
        List<String> warnings
    ) {
        return new AdminWebsiteClubSheetImportResponse(
            importedCount,
            skippedCount,
            warnings == null ? List.of() : warnings
        );
    }
}
