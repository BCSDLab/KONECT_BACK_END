package gg.agit.konect.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.user.dto.SignupRequest;
import gg.agit.konect.domain.user.dto.UserAccessTokenResponse;
import gg.agit.konect.domain.user.dto.UserInfoResponse;
import gg.agit.konect.domain.user.service.RefreshTokenService;
import gg.agit.konect.domain.user.service.SignupTokenService;
import gg.agit.konect.domain.user.service.UserService;
import gg.agit.konect.global.auth.jwt.AccessTokenBlacklistService;
import gg.agit.konect.global.auth.jwt.JwtProvider;
import gg.agit.konect.global.auth.annotation.PublicApi;
import gg.agit.konect.global.auth.annotation.UserId;
import gg.agit.konect.global.auth.web.AuthCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController implements UserApi {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;
    private final SignupTokenService signupTokenService;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieService authCookieService;
    private final AccessTokenBlacklistService accessTokenBlacklistService;

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
    public ResponseEntity<UserInfoResponse> getMyInfo(@UserId Integer userId) {
        UserInfoResponse response = userService.getUserInfo(userId);

        return ResponseEntity.ok(response);
    }

    @Override
    @PublicApi
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = resolveBearerToken(request);
        accessTokenBlacklistService.blacklist(accessToken);

        String refreshToken = authCookieService.getCookieValue(request, AuthCookieService.REFRESH_TOKEN_COOKIE);
        refreshTokenService.revoke(refreshToken);

        authCookieService.clearRefreshToken(request, response);
        authCookieService.clearSignupToken(request, response);

        return ResponseEntity.ok().build();
    }

    @Override
    @PublicApi
    public ResponseEntity<UserAccessTokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String oldAccessToken = resolveBearerToken(request);
        accessTokenBlacklistService.blacklist(oldAccessToken);

        String refreshToken = authCookieService.getCookieValue(request, AuthCookieService.REFRESH_TOKEN_COOKIE);
        RefreshTokenService.Rotated rotated = refreshTokenService.rotate(refreshToken);

        String accessToken = jwtProvider.createToken(rotated.userId());
        authCookieService.setRefreshToken(request, response, rotated.refreshToken(), refreshTokenService.refreshTtl());

        return ResponseEntity.ok(new UserAccessTokenResponse(accessToken));
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
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
}
