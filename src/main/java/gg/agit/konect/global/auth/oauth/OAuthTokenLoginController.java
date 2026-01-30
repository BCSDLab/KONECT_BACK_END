package gg.agit.konect.global.auth.oauth;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.global.auth.annotation.PublicApi;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
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
    public void loginWithToken(
        HttpServletRequest request,
        HttpServletResponse response,
        @RequestBody OAuthTokenLoginRequest body
    ) throws IOException {
        Provider provider = Provider.valueOf(body.provider().toUpperCase());

        OAuthTokenVerifier verifier = verifiers.stream()
            .filter(v -> v.provider() == provider)
            .findFirst()
            .orElseThrow(() -> CustomException.of(ApiResponseCode.UNSUPPORTED_PROVIDER));

        VerifiedOAuthUser verified = verifier.verify(body);

        orchestrator.loginOrSignup(
            request,
            response,
            provider,
            verified.email(),
            verified.providerId(),
            body.redirectUri()
        );
    }
}
