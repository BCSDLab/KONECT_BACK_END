package gg.agit.konect.domain.user.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @NotEmpty(message = "이름은 필수 입력입니다.")
    @Size(max = 30, message = "이름은 최대 30자 입니다.")
    @Pattern(regexp = "^[가-힣]+$", message = "이름은 완성된 한글만 입력할 수 있습니다.")
    @Schema(description = "회원 이름", example = "이동훈", requiredMode = REQUIRED)
    String name,

    @NotEmpty(message = "학번은 필수 입력입니다.")
    @Size(min = 4, max = 20, message = "학번은 4자 이상 20자 이하입니다.")
    @Pattern(regexp = "^[0-9]+$", message = "학번은 숫자만 입력할 수 있습니다.")
    @Schema(description = "회원 학번", example = "2021136091", requiredMode = REQUIRED)
    String studentNumber,

    @Pattern(regexp = "^$|^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
    @Schema(description = "회원 전화번호", example = "010-1234-5678", requiredMode = NOT_REQUIRED)
    String phoneNumber
) {
}
