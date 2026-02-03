package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;

import gg.agit.konect.domain.club.enums.ClubPosition;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubMemberCondition(
    @Schema(description = "직책으로 필터링 (null이면 전체 조회)", example = "PRESIDENT", requiredMode = NOT_REQUIRED)
    ClubPosition position
) {

}
