package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.club.enums.ClubPositionGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClubPositionCreateRequest(
    @NotBlank(message = "직책 이름은 필수 입력입니다.")
    @Size(max = 50, message = "직책 이름은 최대 50자까지 입력 가능합니다.")
    @Schema(description = "직책 이름", example = "부운영진", requiredMode = REQUIRED)
    String name,

    @NotNull(message = "직책 그룹은 필수 입력입니다.")
    @Schema(description = "직책 그룹 (MANAGER 또는 MEMBER만 가능)", example = "MANAGER", requiredMode = REQUIRED)
    ClubPositionGroup positionGroup
) {

}
