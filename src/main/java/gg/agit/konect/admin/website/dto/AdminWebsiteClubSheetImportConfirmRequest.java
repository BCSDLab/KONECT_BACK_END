package gg.agit.konect.admin.website.dto;

import java.util.List;

import gg.agit.konect.domain.club.enums.ClubCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminWebsiteClubSheetImportConfirmRequest(

    @NotEmpty
    List<@Valid ConfirmClub> clubs
) {

    public record ConfirmClub(
        int rowNumber,

        @NotBlank
        @Size(max = 50)
        String name,

        @NotNull
        ClubCategory clubCategory,

        @NotBlank
        @Size(max = 20)
        String topic,

        @NotBlank
        @Size(max = 30)
        String description,

        @NotBlank
        String introduce,

        @NotBlank
        @Size(max = 255)
        String categoryEmoji,

        boolean enabled
    ) {
    }
}
