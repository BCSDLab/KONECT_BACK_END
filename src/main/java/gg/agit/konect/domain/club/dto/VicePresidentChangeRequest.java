package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record VicePresidentChangeRequest(
    @Schema(description = "부회장으로 임명할 사용자 ID (null이면 부회장 해제)", example = "3", requiredMode = NOT_REQUIRED)
    Integer vicePresidentUserId
) {

}
