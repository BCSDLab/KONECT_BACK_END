package gg.agit.konect.admin.version.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.version.enums.PlatformType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminVersionCreateRequest(
    @NotNull(message = "플랫폼은 필수 입력입니다.")
    @Schema(description = "플랫폼 타입", example = "IOS", requiredMode = REQUIRED)
    PlatformType platform,

    @NotBlank(message = "버전은 필수 입력입니다.")
    @Schema(description = "버전", example = "1.2.3", requiredMode = REQUIRED)
    String version,

    @Schema(description = "릴리즈 노트")
    String releaseNotes
) {
}
