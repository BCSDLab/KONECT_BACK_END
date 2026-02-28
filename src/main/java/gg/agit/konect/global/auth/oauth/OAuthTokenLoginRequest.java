package gg.agit.konect.global.auth.oauth;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record OAuthTokenLoginRequest(
    @NotBlank(message = "OAuth 제공자는 필수 입력입니다.")
    @Schema(description = "OAuth 제공자", example = "GOOGLE", requiredMode = REQUIRED)
    String provider,

    @Schema(description = "OAuth 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...", requiredMode = NOT_REQUIRED)
    String accessToken,

    @Schema(
        description = "OAuth 아이디 토큰",
        example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
        requiredMode = NOT_REQUIRED
    )
    String idToken,

    @Schema(description = "리다이렉트 경로", example = "https://agit.gg", requiredMode = NOT_REQUIRED)
    String redirectUri,

    @Schema(description = "애플 인증에서 전달된 이름", example = "홍길동", requiredMode = NOT_REQUIRED)
    String name
) {
}
