package gg.agit.konect.domain.user.dto;


import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @NotEmpty(message = "이름은 필수 입력입니다.")
    @Size(max = 50, message = "이름은 최대 50자 입니다.")
    @Schema(description = "회원 이름", example = "이동훈", requiredMode = REQUIRED)
    String name,

    @NotEmpty(message = "학번은 필수 입력입니다.")
    @Size(max = 20, message = "학번은 최대 20자 입니다.")
    @Pattern(regexp = "^\\S+$", message = "학번은 공백을 포함할 수 없습니다.")
    @Schema(description = "회원 학번", example = "2021136091", requiredMode = REQUIRED)
    String studentNumber,

    @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
    @Schema(description = "회원 전화번호", example = "010-1234-5678", requiredMode = NOT_REQUIRED)
    String phoneNumber
) {
}
