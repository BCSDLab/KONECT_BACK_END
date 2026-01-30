package gg.agit.konect.infra.oauth;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.global.auth.oauth.OAuthTokenLoginRequest;
import gg.agit.konect.global.auth.oauth.OAuthTokenVerifier;
import gg.agit.konect.global.auth.oauth.VerifiedOAuthUser;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GoogleTokenVerifier implements OAuthTokenVerifier {

    // Google JWKS (공개키) 주소: 서명검증에 사용
    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    String googleClientId;

    private final JwtDecoder jwtDecoder;

    public GoogleTokenVerifier(@Value("${OAUTH_GOOGLE_CLIENT_ID}") String googleClientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URI).build();

        OAuth2TokenValidator<Jwt> issuerValidator = token -> {
            String iss = token.getIssuer() != null ? token.getIssuer().toString() : null;

            if (GOOGLE_ISSUER.equals(iss)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Invalid issuer: " + iss, null)
            );
        };

        // (기존 그대로) aud 검증이 필요하면 유지
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtAudienceValidator(googleClientId);

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            issuerValidator,
            audienceValidator
        ));

        this.jwtDecoder = decoder;
    }

    @Override
    public Provider provider() {
        return Provider.GOOGLE;
    }

    @Override
    public VerifiedOAuthUser verify(OAuthTokenLoginRequest req) {
        String idToken = req.idToken();

        if (!StringUtils.hasText(idToken)) {
            throw CustomException.of(ApiResponseCode.INVALID_OAUTH_TOKEN);
        }

        try {
            Jwt jwt = jwtDecoder.decode(idToken);

            String sub = jwt.getSubject();           // providerId
            String email = jwt.getClaimAsString("email");

            if (!StringUtils.hasText(sub)) {
                throw CustomException.of(ApiResponseCode.FAILED_EXTRACT_PROVIDER_ID);
            }
            if (!StringUtils.hasText(email)) {
                throw CustomException.of(ApiResponseCode.FAILED_EXTRACT_EMAIL);
            }

            return new VerifiedOAuthUser(sub, email);
        } catch (JwtException e) {
            log.error(e.getMessage(), e);
            throw CustomException.of(ApiResponseCode.INVALID_OAUTH_TOKEN);
        }
    }

    private OAuth2TokenValidator<Jwt> audienceValidator(String expectedAud) {
        return token -> {
            List<String> aud = token.getAudience();
            if (aud != null && aud.contains(expectedAud)) {
                return OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error = new OAuth2Error("invalid_token", "Invalid audience", null);
            return OAuth2TokenValidatorResult.failure(error);
        };
    }
}
