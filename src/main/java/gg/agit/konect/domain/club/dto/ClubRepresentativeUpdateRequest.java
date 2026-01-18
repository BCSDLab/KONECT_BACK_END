package gg.agit.konect.domain.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ClubRepresentativeUpdateRequest(
    @Schema(description = "새로운 회장의 사용자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "사용자 ID는 필수입니다.")
    Integer userId
) {
}
