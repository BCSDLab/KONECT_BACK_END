package gg.agit.konect.domain.user.dto;

import gg.agit.konect.domain.user.enums.Provider;

public record OAuthProviderLinkStatus(
    Provider provider,
    boolean linked
) {
}
