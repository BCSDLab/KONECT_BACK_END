package gg.agit.konect.user.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.user.dto.SignupRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Tag(name = "(Normal) User: 유저", description = "유저 API")
@RequestMapping("/users")
public interface UserApi {

    @Operation(summary = "추가 정보가 필요한 사용자의 정보를 받아 회원가입을 진행한다.")
    @PostMapping("/signup")
    void signup(HttpSession session, @RequestBody @Valid SignupRequest request);
}
