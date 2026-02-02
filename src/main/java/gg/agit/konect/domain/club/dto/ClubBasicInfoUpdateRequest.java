package gg.agit.konect.domain.club.dto;

import gg.agit.konect.domain.club.enums.ClubCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClubBasicInfoUpdateRequest(
    @Schema(description = "동아리 이름", example = "BCSD Lab", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "동아리 이름은 필수 입력입니다.")
    @Size(max = 50, message = "동아리 이름은 50자 이하여야 합니다.")
    String name,

    @Schema(description = "동아리 분과", example = "ACADEMIC", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "동아리 분과는 필수 입력입니다.")
    ClubCategory clubCategory
) {
}
