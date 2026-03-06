package gg.agit.konect.domain.user.dto;

import java.util.List;

public record OAuthLinkStatusResponse(
    List<OAuthProviderLinkStatus> providers
) {
}
