package gg.agit.konect.domain.version.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.version.model.Version;
import io.swagger.v3.oas.annotations.media.Schema;

public record VersionResponse(
    @Schema(description = "플랫폼 타입", example = "IOS", requiredMode = REQUIRED)
    String platform,

    @Schema(description = "최신 버전", example = "1.2.3", requiredMode = REQUIRED)
    String version,

    @Schema(description = "릴리즈 노트")
    String releaseNotes
) {
    public static VersionResponse from(Version version) {
        return new VersionResponse(
            version.getPlatform().name(),
            version.getVersion(),
            version.getReleaseNotes()
        );
    }
}
