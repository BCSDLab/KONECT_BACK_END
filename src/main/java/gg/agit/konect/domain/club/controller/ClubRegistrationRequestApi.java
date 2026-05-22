package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.club.dto.ClubRegistrationRequest;
import gg.agit.konect.global.auth.annotation.PublicApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Club - Registration Request: 신규 동아리 등록 요청")
@RequestMapping("/clubs/registration-requests")
public interface ClubRegistrationRequestApi {

    @Operation(summary = "신규 동아리 등록 요청을 보낸다.", description = """
        로그인하지 않은 사용자도 신규 동아리 등록 요청을 보낼 수 있습니다.
        요청 내용은 가입/탈퇴 알림과 같은 Slack event webhook으로 전달됩니다.
        """)
    @PostMapping
    @PublicApi
    ResponseEntity<Void> submitClubRegistrationRequest(@Valid @RequestBody ClubRegistrationRequest request);
}
