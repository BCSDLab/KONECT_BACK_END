package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.club.enums.ClubPosition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClubPreMemberAddRequest(
    @NotEmpty(message = "학번은 필수 입력입니다.")
    @Size(min = 4, max = 20, message = "학번은 4자 이상 20자 이하입니다.")
    @Pattern(regexp = "^[0-9]+$", message = "학번은 숫자만 입력할 수 있습니다.")
    @Schema(description = "사전 등록할 회원의 학번", example = "2021136089", requiredMode = REQUIRED)
    String studentNumber,

    @NotEmpty(message = "이름은 필수 입력입니다.")
    @Size(min = 2, max = 5, message = "이름은 2자 이상 5자 이하 입니다.")
    @Pattern(regexp = "^[가-힣]+$", message = "이름은 완성된 한글만 입력할 수 있습니다.")
    @Schema(description = "사전 등록할 회원 이름", example = "홍길동", requiredMode = REQUIRED)
    String name,

    @Schema(description = "사전 등록 회원의 가입 직책 (미입력 시 MEMBER)", example = "MEMBER", requiredMode = NOT_REQUIRED)
    ClubPosition clubPosition
) {
}
