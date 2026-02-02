package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record MemberPositionChangeRequest(
    @NotNull(message = "직책 ID는 필수 입력입니다.")
    @Schema(description = "변경할 직책 ID", example = "3", requiredMode = REQUIRED)
    Integer positionId
) {

}
