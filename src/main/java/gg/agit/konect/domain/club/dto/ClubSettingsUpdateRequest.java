package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동아리 설정 변경 요청")
public record ClubSettingsUpdateRequest(
    @Schema(description = "모집공고 활성화 여부", example = "true", requiredMode = NOT_REQUIRED)
    Boolean isRecruitmentEnabled,

    @Schema(description = "지원서 활성화 여부", example = "true", requiredMode = NOT_REQUIRED)
    Boolean isApplicationEnabled,

    @Schema(description = "회비 활성화 여부", example = "false", requiredMode = NOT_REQUIRED)
    Boolean isFeeEnabled
) {
}
