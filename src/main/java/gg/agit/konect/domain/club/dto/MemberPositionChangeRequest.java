package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.club.enums.ClubPosition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record MemberPositionChangeRequest(
    @NotNull(message = "직책은 필수 입력입니다.")
    @Schema(description = "변경할 직책", example = "MEMBER", requiredMode = REQUIRED)
    ClubPosition position
) {

}
