package gg.agit.konect.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.security.enums.Provider;
import gg.agit.konect.user.dto.MyInfoResponse;
import gg.agit.konect.user.dto.SignupRequest;
import gg.agit.konect.user.model.User;
import gg.agit.konect.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController implements UserApi {

    private final UserService userService;

    @PostMapping("/signup")
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

        User user = userService.signup(email, provider, request);

        session.invalidate();

        HttpSession newSession = httpServletRequest.getSession(true);
        newSession.setAttribute("userId", user.getId());

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MyInfoResponse> getMyInfo(HttpSession session) {
        Integer userId = (Integer)session.getAttribute("userId");

        MyInfoResponse response = userService.getUserInfo(userId);

        return ResponseEntity.ok(response);
    }
}
