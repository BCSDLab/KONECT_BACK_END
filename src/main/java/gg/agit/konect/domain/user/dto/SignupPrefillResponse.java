package gg.agit.konect.domain.user.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record SignupPrefillResponse(
    @Schema(description = "회원가입 사전 입력 이름", example = "홍길동", requiredMode = NOT_REQUIRED)
    String name
) {
}
