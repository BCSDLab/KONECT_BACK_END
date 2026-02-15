package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;

import gg.agit.konect.domain.club.enums.ClubPosition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ClubPreMemberAddRequest(
    @NotBlank(message = "학번은 필수 입력입니다.")
    @Schema(description = "사전 등록할 회원의 학번", example = "2021136089", requiredMode = REQUIRED)
    String studentNumber,

    @NotBlank(message = "이름은 필수 입력입니다.")
    @Schema(description = "사전 등록할 회원의 이름", example = "홍길동", requiredMode = REQUIRED)
    String name,

    @Schema(description = "사전 등록 회원의 가입 직책 (미입력 시 MEMBER)", example = "MEMBER", requiredMode = NOT_REQUIRED)
    ClubPosition clubPosition
) {
}
