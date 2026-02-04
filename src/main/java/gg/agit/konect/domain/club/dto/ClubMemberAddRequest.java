package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ClubMemberAddRequest(
    @NotBlank(message = "학번은 필수 입력입니다.")
    @Schema(description = "사전 등록할 회원의 학번", example = "2021136089", requiredMode = REQUIRED)
    String studentNumber,

    @NotBlank(message = "이름은 필수 입력입니다.")
    @Schema(description = "사전 등록할 회원의 이름", example = "홍길동", requiredMode = REQUIRED)
    String name
) {
}
