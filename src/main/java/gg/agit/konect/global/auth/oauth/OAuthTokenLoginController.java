package gg.agit.konect.global.auth.oauth;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.global.auth.annotation.PublicApi;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/oauth")
public class OAuthTokenLoginController {

    private final List<OAuthTokenVerifier> verifiers;
    private final OAuthLoginOrchestrator orchestrator;

    @PublicApi
    @PostMapping("/token")
    public ResponseEntity<OAuthTokenLoginResponse> loginWithToken(
        HttpServletRequest request,
        HttpServletResponse response,
        @Valid @RequestBody OAuthTokenLoginRequest body
    ) throws IOException {
        Provider provider = resolveProvider(body.provider());

        OAuthTokenVerifier verifier = verifiers.stream()
            .filter(v -> v.provider() == provider)
            .findFirst()
            .orElseThrow(() -> CustomException.of(ApiResponseCode.UNSUPPORTED_PROVIDER));

        VerifiedOAuthUser verified = verifier.verify(body);
        String oauthName = resolveOAuthName(provider, verified.name(), body.name());

        return ResponseEntity.ok(
            orchestrator.loginOrSignup(
                request,
                response,
                provider,
                verified.email(),
                verified.providerId(),
                oauthName,
                body.redirectUri()
            )
        );
    }

    private String resolveOAuthName(Provider provider, String verifiedName, String requestName) {
        if (StringUtils.hasText(verifiedName)) {
            return verifiedName;
        }

        if (provider == Provider.APPLE && StringUtils.hasText(requestName)) {
            return requestName;
        }

        return null;
    }

    private Provider resolveProvider(String rawProvider) {
        try {
            return Provider.valueOf(rawProvider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw CustomException.of(ApiResponseCode.UNSUPPORTED_PROVIDER);
        }
    }
}
