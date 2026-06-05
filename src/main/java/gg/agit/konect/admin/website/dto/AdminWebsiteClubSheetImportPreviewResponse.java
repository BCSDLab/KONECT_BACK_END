package gg.agit.konect.admin.website.dto;

import java.util.List;

import gg.agit.konect.domain.club.enums.ClubCategory;

public record AdminWebsiteClubSheetImportPreviewResponse(
    Integer universityId,
    int previewCount,
    List<PreviewClub> clubs,
    List<String> warnings
) {

    public static AdminWebsiteClubSheetImportPreviewResponse of(
        Integer universityId,
        List<PreviewClub> clubs,
        List<String> warnings
    ) {
        return new AdminWebsiteClubSheetImportPreviewResponse(
            universityId,
            clubs.size(),
            clubs,
            warnings == null ? List.of() : warnings
        );
    }

    public record PreviewClub(
        int rowNumber,
        String name,
        ClubCategory clubCategory,
        String topic,
        String description,
        String introduce,
        String categoryEmoji,
        boolean enabled
    ) {
    }
}
