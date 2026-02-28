package gg.agit.konect.infrastructure.oauth;

import java.util.Map;

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
public class AppleTokenVerifier implements OAuthTokenVerifier {

    private static final String APPLE_JWKS_URI = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final JwtDecoder jwtDecoder;

    public AppleTokenVerifier(@Value("${OAUTH_APPLE_CLIENT_ID}") String appleClientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(APPLE_JWKS_URI).build();

        OAuth2TokenValidator<Jwt> issuerValidator = token -> {
            String iss = token.getIssuer() != null ? token.getIssuer().toString() : null;

            if (APPLE_ISSUER.equals(iss)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Invalid issuer: " + iss, null)
            );
        };

        OAuth2TokenValidator<Jwt> audienceValidator = new JwtAudienceValidator(appleClientId);

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            issuerValidator,
            audienceValidator
        ));

        this.jwtDecoder = decoder;
    }

    @Override
    public Provider provider() {
        return Provider.APPLE;
    }

    @Override
    public VerifiedOAuthUser verify(OAuthTokenLoginRequest req) {
        String idToken = req.idToken();

        if (!StringUtils.hasText(idToken)) {
            throw CustomException.of(ApiResponseCode.INVALID_OAUTH_TOKEN);
        }

        try {
            Jwt jwt = jwtDecoder.decode(idToken);

            String providerId = jwt.getSubject();

            if (!StringUtils.hasText(providerId)) {
                throw CustomException.of(ApiResponseCode.FAILED_EXTRACT_PROVIDER_ID);
            }

            String email = jwt.getClaimAsString("email");
            String name = extractName(jwt);

            return new VerifiedOAuthUser(providerId, email, name);
        } catch (JwtException e) {
            log.error(e.getMessage(), e);
            throw CustomException.of(ApiResponseCode.INVALID_OAUTH_TOKEN);
        }
    }

    private String extractName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");

        if (StringUtils.hasText(name)) {
            return name;
        }

        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");

        if (StringUtils.hasText(givenName) && StringUtils.hasText(familyName)) {
            return familyName + givenName;
        }

        if (StringUtils.hasText(givenName)) {
            return givenName;
        }

        if (StringUtils.hasText(familyName)) {
            return familyName;
        }

        Object rawName = jwt.getClaims().get("name");

        if (!(rawName instanceof Map<?, ?> nameMap)) {
            return null;
        }

        String firstName = asText(nameMap.get("firstName"));
        String lastName = asText(nameMap.get("lastName"));

        if (StringUtils.hasText(firstName) && StringUtils.hasText(lastName)) {
            return lastName + firstName;
        }

        if (StringUtils.hasText(firstName)) {
            return firstName;
        }

        if (StringUtils.hasText(lastName)) {
            return lastName;
        }

        return null;
    }

    private String asText(Object value) {
        if (value instanceof String text && StringUtils.hasText(text)) {
            return text;
        }

        return null;
    }
}
