package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record PresidentTransferRequest(
    @NotNull(message = "새 회장의 사용자 ID는 필수 입력입니다.")
    @Schema(description = "새 회장으로 임명할 사용자 ID", example = "2", requiredMode = REQUIRED)
    Integer newPresidentUserId
) {

}
