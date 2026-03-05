package gg.agit.konect.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthLinkRequest(
    @NotBlank(message = "OAuth 제공자는 필수 입력입니다.")
    String provider,

    String accessToken,

    String idToken,

    String name
) {
}
