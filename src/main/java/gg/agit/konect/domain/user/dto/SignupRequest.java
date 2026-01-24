package gg.agit.konect.domain.user.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotEmpty(message = "이름은 필수 입력입니다.")
    @Size(max = 30, message = "이름은 최대 30자 입니다.")
    @Pattern(regexp = "^[가-힣]+$", message = "이름은 완성된 한글만 입력할 수 있습니다.")
    @Schema(description = "회원 이름", example = "홍길동", requiredMode = REQUIRED)
    String name,

    @NotNull(message = "학교 id는 필수 입력입니다.")
    @Schema(description = "학교 id", example = "1", requiredMode = REQUIRED)
    Integer universityId,

    @NotEmpty(message = "학번은 필수 입력입니다.")
    @Size(min = 4, max = 20, message = "학번은 4자 이상 20자 이하입니다.")
    @Pattern(regexp = "^[0-9]+$", message = "학번은 숫자만 입력할 수 있습니다.")
    @Schema(description = "회원 학번", example = "20250001", requiredMode = REQUIRED)
    String studentNumber,

    @NotNull(message = "마케팅 동의 여부는 필수입니다.")
    @Schema(description = "마케팅 수신 동의 여부", example = "true", requiredMode = REQUIRED)
    Boolean isMarketingAgreement
) {
}
