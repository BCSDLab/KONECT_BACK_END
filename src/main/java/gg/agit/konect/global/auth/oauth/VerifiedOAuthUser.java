package gg.agit.konect.global.auth.oauth;

public record VerifiedOAuthUser(
    String providerId,
    String email,
    String name
) {

}
