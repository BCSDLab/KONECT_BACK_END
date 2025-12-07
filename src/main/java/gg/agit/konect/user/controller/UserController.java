package gg.agit.konect.user.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.security.enums.Provider;
import gg.agit.konect.user.dto.SignupRequest;
import gg.agit.konect.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController implements UserApi {

    private final UserService userService;

    @PostMapping("/signup")
    public void signup(
        HttpSession session,
        @RequestBody @Valid SignupRequest request
    ) {
        String email = (String) session.getAttribute("email");
        Provider provider = Provider.valueOf(((String)session.getAttribute("provider")).toUpperCase());
        Boolean isRegistered = (Boolean) session.getAttribute("isRegistered");

        if (email == null || isRegistered == null) {
            throw CustomException.of(ApiResponseCode.INVALID_SESSION);
        }

        if (isRegistered) {
            throw CustomException.of(ApiResponseCode.ALREADY_REGISTERED_USER);
        }

        userService.signup(email, provider, request);
    }
}
