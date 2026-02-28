package gg.agit.konect.global.auth.oauth;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record OAuthTokenLoginResponse(
    @Schema(description = "리다이렉트 경로", example = "https://agit.gg", requiredMode = REQUIRED)
    String redirectUri,

    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...", requiredMode = NOT_REQUIRED)
    String accessToken,

    @Schema(description = "리프레시 토큰", example = "IQxiK3gPhYIP8...", requiredMode = NOT_REQUIRED)
    String refreshToken,

    @Schema(description = "회원가입 토큰", example = "eyJhbGciOiJIUzI1NiJ9...", requiredMode = NOT_REQUIRED)
    String signupToken,

    @Schema(description = "OAuth 제공자 계정 이름", example = "홍길동", requiredMode = NOT_REQUIRED)
    String name
) {
    public static OAuthTokenLoginResponse login(String redirectUri, String accessToken, String refreshToken) {
        return new OAuthTokenLoginResponse(redirectUri, accessToken, refreshToken, null, null);
    }

    public static OAuthTokenLoginResponse signup(String redirectUri, String signupToken, String name) {
        return new OAuthTokenLoginResponse(redirectUri, null, null, signupToken, name);
    }
}
