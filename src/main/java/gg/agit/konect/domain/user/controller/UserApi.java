package gg.agit.konect.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.user.dto.SignupRequest;
import gg.agit.konect.domain.user.dto.UserInfoResponse;
import gg.agit.konect.domain.user.dto.UserUpdateRequest;
import gg.agit.konect.global.auth.annotation.PublicApi;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Tag(name = "(Normal) User: 유저", description = "유저 API")
@RequestMapping("/users")
public interface UserApi {

    @Operation(
        summary = "추가 정보가 필요한 사용자의 정보를 받아 회원가입을 진행한다.",
        description = """
            추가 정보를 입력받아 회원가입을 완료합니다.

            - `INVALID_REQUEST_BODY` (400): 요청 본문의 형식이 올바르지 않거나 필수 값이 누락된 경우
            - `DUPLICATE_STUDENT_NUMBER` (409): 동일 대학교 + 학번 조합이 이미 존재하는 경우
            - `ALREADY_REGISTERED_USER` (409): 이미 가입된 회원인 경우
            - `UNIVERSITY_NOT_FOUND` (404): 대학교를 찾을 수 없는 경우
            - `NOT_FOUND_UNREGISTERED_USER` (404): 임시 유저를 찾을 수 없는 경우
            """
    )
    @PostMapping("/signup")
    @PublicApi
    ResponseEntity<Void> signup(
        HttpServletRequest request,
        HttpServletResponse response,
        @RequestBody @Valid SignupRequest signupRequest
    );

    @Operation(summary = "로그인한 사용자의 정보를 조회한다.")
    @GetMapping("/me")
    ResponseEntity<UserInfoResponse> getMyInfo(@UserId Integer userId);

    @Operation(summary = "로그인한 사용자의 정보를 수정한다.")
    @PutMapping("/me")
    ResponseEntity<Void> updateMyInfo(
        @UserId Integer userId,
        @RequestBody @Valid UserUpdateRequest request
    );

    @Operation(summary = "로그아웃한다.")
    @PostMapping("/logout")
    @PublicApi
    ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response);

    @Operation(summary = "리프레시 토큰으로 액세스 토큰을 재발급한다.")
    @PostMapping("/refresh")
    @PublicApi
    ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response);

    @Operation(summary = "회원탈퇴를 한다.")
    @DeleteMapping("/withdraw")
    ResponseEntity<Void> withdraw(HttpServletRequest request, HttpServletResponse response, @UserId Integer userId);
}
