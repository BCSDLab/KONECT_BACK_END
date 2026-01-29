package gg.agit.konect.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

import gg.agit.konect.domain.user.dto.SignupRequest;
import gg.agit.konect.domain.user.dto.UserInfoResponse;
import gg.agit.konect.domain.user.dto.UserUpdateRequest;
import gg.agit.konect.domain.user.service.UserService;
import gg.agit.konect.global.auth.annotation.PublicApi;
import gg.agit.konect.global.auth.annotation.UserId;
import gg.agit.konect.global.auth.JwtProvider;
import gg.agit.konect.global.auth.token.AuthCookieService;
import gg.agit.konect.domain.user.service.RefreshTokenService;
import gg.agit.konect.domain.user.service.SignupTokenService;
import jakarta.servlet.http.Cookie;
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
    private final AuthCookieService authCookieService;

    @Override
    @PublicApi
    public ResponseEntity<Void> signup(
        HttpServletRequest request,
        HttpServletResponse response,
        @RequestBody @Valid SignupRequest signupRequest
    ) {
        String signupToken = getCookieValue(request, AuthCookieService.SIGNUP_TOKEN_COOKIE);
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
    public ResponseEntity<Void> updateMyInfo(
        @UserId Integer userId,
        @RequestBody @Valid UserUpdateRequest request
    ) {
        userService.updateUserInfo(userId, request);

        return ResponseEntity.ok().build();
    }

    @Override
    @PublicApi
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getCookieValue(request, AuthCookieService.REFRESH_TOKEN_COOKIE);
        refreshTokenService.revoke(refreshToken);

        authCookieService.clearRefreshToken(request, response);
        authCookieService.clearSignupToken(request, response);

        return ResponseEntity.ok().build();
    }

    @Override
    @PublicApi
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getCookieValue(request, AuthCookieService.REFRESH_TOKEN_COOKIE);
        RefreshTokenService.Rotated rotated = refreshTokenService.rotate(refreshToken);

        String accessToken = jwtProvider.createToken(rotated.userId());
        response.setHeader("Authorization", "Bearer " + accessToken);
        authCookieService.setRefreshToken(request, response, rotated.refreshToken(), refreshTokenService.refreshTtl());

        return ResponseEntity.ok().build();
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

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie cookie = WebUtils.getCookie(request, name);
        return cookie == null ? null : cookie.getValue();
    }
}
