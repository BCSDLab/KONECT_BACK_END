package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.club.enums.ClubPosition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ClubMemberAddRequest(
    @NotNull(message = "사용자 ID는 필수 입력입니다.")
    @Schema(description = "추가할 사용자 ID", example = "123", requiredMode = REQUIRED)
    Integer userId,

    @NotNull(message = "직책은 필수 입력입니다.")
    @Schema(description = "부여할 직책", example = "MEMBER", requiredMode = REQUIRED)
    ClubPosition position
) {
}
