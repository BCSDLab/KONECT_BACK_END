package gg.agit.konect.user.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotEmpty(message = "이름은 필수 입력입니다.")
    @Size(max = 50, message = "이름은 최대 50자 입니다.")
    @Schema(description = "회원 이름", example = "홍길동", requiredMode = REQUIRED)
    String name,

    @NotEmpty(message = "학교명은 필수 입력입니다.")
    @Size(max = 100, message = "학교명은 최대 100자 입니다.")
    @Schema(description = "학교명", example = "한국기술교육대학교", requiredMode = REQUIRED)
    String schoolName,

    @NotEmpty(message = "학번은 필수 입력입니다.")
    @Size(max = 20, message = "학번은 최대 20자 입니다.")
    @Schema(description = "회원 학번", example = "20250001", requiredMode = REQUIRED)
    String studentNumber,

    @NotNull(message = "마케팅 동의 여부는 필수입니다.")
    @Schema(description = "마케팅 수신 동의 여부", example = "true", requiredMode = REQUIRED)
    Boolean marketingAgreement
) {
}
