package gg.agit.konect.global.auth.oauth;

public record OAuthTokenLoginResponse(
    String redirectUri,
    String refreshToken,
    String signupToken
) {
    public static OAuthTokenLoginResponse login(String redirectUri, String refreshToken) {
        return new OAuthTokenLoginResponse(redirectUri, refreshToken, null);
    }

    public static OAuthTokenLoginResponse signup(String redirectUri, String signupToken) {
        return new OAuthTokenLoginResponse(redirectUri, null, signupToken);
    }
}
