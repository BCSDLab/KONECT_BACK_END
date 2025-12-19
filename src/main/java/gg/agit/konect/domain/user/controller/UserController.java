package gg.agit.konect.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.user.dto.SignupRequest;
import gg.agit.konect.domain.user.dto.UserInfoResponse;
import gg.agit.konect.domain.user.dto.UserUpdateRequest;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.service.UserService;
import gg.agit.konect.global.auth.annotation.UserId;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    public ResponseEntity<Void> signup(
        HttpServletRequest httpServletRequest,
        HttpSession session,
        @RequestBody @Valid SignupRequest request
    ) {
        String email = (String)session.getAttribute("email");
        Provider provider = (Provider)session.getAttribute("provider");

        if (email == null || provider == null) {
            throw CustomException.of(ApiResponseCode.INVALID_SESSION);
        }

        Integer userId = userService.signup(email, provider, request);

        session.invalidate();

        HttpSession newSession = httpServletRequest.getSession(true);
        newSession.setAttribute("userId", userId);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<UserInfoResponse> getMyInfo(@UserId Integer userId) {
        UserInfoResponse response = userService.getUserInfo(userId);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> updateMyInfo(
        HttpSession session,
        @RequestBody @Valid UserUpdateRequest request
    ) {
        Integer userId = (Integer)session.getAttribute("userId");

        userService.updateUserInfo(userId, request);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok().build();
    }
}
