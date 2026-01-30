package gg.agit.konect.global.auth.oauth;

import gg.agit.konect.domain.user.enums.Provider;

public interface OAuthTokenVerifier {
    Provider provider();
    VerifiedOAuthUser verify(OAuthTokenLoginRequest req);
}
