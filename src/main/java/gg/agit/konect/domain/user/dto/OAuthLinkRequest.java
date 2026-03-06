package gg.agit.konect.domain.user.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record OAuthLinkRequest(
    @NotBlank(message = "OAuth 제공자는 필수 입력입니다.")
    @Schema(description = "OAuth 제공자", example = "google", requiredMode = REQUIRED)
    String provider,

    @Schema(
        description = "OAuth access token (provider별로 필요 여부가 다름)",
        example = "eyJhbGciOi...",
        requiredMode = NOT_REQUIRED
    )
    String accessToken,

    @Schema(
        description = "OAuth id token (provider별로 필요 여부가 다름)",
        example = "eyJraWQiOi...",
        requiredMode = NOT_REQUIRED
    )
    String idToken,

    @Schema(description = "OAuth 제공자에서 전달된 사용자 이름", example = "홍길동")
    String name
) {
}
