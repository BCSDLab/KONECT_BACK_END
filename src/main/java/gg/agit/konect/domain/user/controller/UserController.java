package gg.agit.konect.domain.user.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.user.dto.OAuthLinkRequest;
import gg.agit.konect.domain.user.dto.OAuthLinkStatusResponse;
import gg.agit.konect.domain.user.dto.SignupRequest;
import gg.agit.konect.domain.user.dto.SignupPrefillResponse;
import gg.agit.konect.domain.user.dto.UserAccessTokenResponse;
import gg.agit.konect.domain.user.dto.UserInfoResponse;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.service.RefreshTokenService;
import gg.agit.konect.domain.user.service.SignupTokenService;
import gg.agit.konect.domain.user.service.UserActivityService;
import gg.agit.konect.domain.user.service.UserOAuthAccountService;
import gg.agit.konect.domain.user.service.UserService;
import gg.agit.konect.global.auth.jwt.JwtProvider;
import gg.agit.konect.global.auth.annotation.PublicApi;
import gg.agit.konect.global.auth.annotation.UserId;
import gg.agit.konect.global.auth.oauth.OAuthTokenLoginRequest;
import gg.agit.konect.global.auth.oauth.OAuthTokenVerifier;
import gg.agit.konect.global.auth.web.AuthCookieService;
import gg.agit.konect.global.auth.oauth.VerifiedOAuthUser;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController implements UserApi {

    private final UserService userService;
    private final SignupTokenService signupTokenService;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserActivityService userActivityService;
    private final AuthCookieService authCookieService;
    private final UserOAuthAccountService userOAuthAccountService;
    private final List<OAuthTokenVerifier> verifiers;

    @Override
    @PublicApi
    public ResponseEntity<Void> signup(
        HttpServletRequest request,
        HttpServletResponse response,
        @RequestBody @Valid SignupRequest signupRequest
    ) {
        String signupToken = authCookieService.getCookieValue(request, AuthCookieService.SIGNUP_TOKEN_COOKIE);
        SignupTokenService.SignupClaims claims = signupTokenService.consumeOrThrow(signupToken);

        Integer userId = userService.signup(claims.email(), claims.providerId(), claims.provider(), signupRequest);

        authCookieService.clearSignupToken(request, response);

        String refreshToken = refreshTokenService.issue(userId);
        authCookieService.setRefreshToken(request, response, refreshToken, refreshTokenService.refreshTtl());

        String accessToken = jwtProvider.createToken(userId);
        response.setHeader("Authorization", "Bearer " + accessToken);

        return ResponseEntity.ok().build();
    }

    @Override
    @PublicApi
    public ResponseEntity<SignupPrefillResponse> getSignupPrefill(HttpServletRequest request) {
        String signupToken = authCookieService.getCookieValue(request, AuthCookieService.SIGNUP_TOKEN_COOKIE);
        SignupTokenService.SignupClaims claims = signupTokenService.readOrThrow(signupToken);

        return ResponseEntity.ok(new SignupPrefillResponse(claims.name()));
    }

    @Override
    public ResponseEntity<UserInfoResponse> getMyInfo(@UserId Integer userId) {
        UserInfoResponse response = userService.getUserInfo(userId);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<OAuthLinkStatusResponse> getOAuthLinkStatus(@UserId Integer userId) {
        OAuthLinkStatusResponse response = userOAuthAccountService.getLinkStatus(userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> linkOAuthAccount(@UserId Integer userId, @RequestBody @Valid OAuthLinkRequest request) {
        Provider provider = resolveProvider(request.provider());

        OAuthTokenVerifier verifier = verifiers.stream()
            .filter(v -> v.provider() == provider)
            .findFirst()
            .orElseThrow(() -> CustomException.of(ApiResponseCode.UNSUPPORTED_PROVIDER));

        VerifiedOAuthUser verified = verifier.verify(
            new OAuthTokenLoginRequest(
                request.provider(),
                request.accessToken(),
                request.idToken(),
                null,
                request.name()
            )
        );

        userOAuthAccountService.linkOAuthAccount(userId, provider, verified.providerId(), verified.email(), null);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PublicApi
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authCookieService.clearRefreshToken(request, response);
        authCookieService.clearSignupToken(request, response);

        return ResponseEntity.ok().build();
    }

    @Override
    @PublicApi
    public ResponseEntity<UserAccessTokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = authCookieService.getCookieValue(request, AuthCookieService.REFRESH_TOKEN_COOKIE);
        RefreshTokenService.Rotated rotated = refreshTokenService.rotate(refreshToken);
        userActivityService.updateLastLoginAt(rotated.userId());

        String accessToken = jwtProvider.createToken(rotated.userId());
        authCookieService.setRefreshToken(request, response, rotated.refreshToken(), refreshTokenService.refreshTtl());

        return ResponseEntity.ok(new UserAccessTokenResponse(accessToken));
    }

    @Override
    public ResponseEntity<Void> withdraw(
        HttpServletRequest request,
        HttpServletResponse response,
        @UserId Integer userId
    ) {
        userService.deleteUser(userId);
        logout(request, response);

        return ResponseEntity.noContent().build();
    }

    private Provider resolveProvider(String rawProvider) {
        if (!StringUtils.hasText(rawProvider)) {
            throw CustomException.of(ApiResponseCode.UNSUPPORTED_PROVIDER);
        }

        try {
            return Provider.valueOf(rawProvider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw CustomException.of(ApiResponseCode.UNSUPPORTED_PROVIDER);
        }
    }
}
