package gg.agit.konect.infrastructure.oauth.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record GoogleDriveAuthorizationUrlResponse(
    @Schema(
        description = "Google Drive 권한 연결을 위해 브라우저를 이동시킬 authorize URL",
        example = "https://accounts.google.com/o/oauth2/v2/auth?client_id=example&redirect_uri=https://api.stage.agit.gg/auth/oauth/google/drive/callback&response_type=code&scope=https://www.googleapis.com/auth/drive&access_type=offline&prompt=consent&state=example-state",
        requiredMode = REQUIRED
    )
    String authorizationUrl
) {
}
