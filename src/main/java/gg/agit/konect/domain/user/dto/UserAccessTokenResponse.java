package gg.agit.konect.domain.user.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserAccessTokenResponse(
    @Schema(
        description = "액세스 토큰",
        example = "eyJhbGciOiJIUzI1NiJ9...",
        requiredMode = REQUIRED
    )
    String accessToken
) {
}
