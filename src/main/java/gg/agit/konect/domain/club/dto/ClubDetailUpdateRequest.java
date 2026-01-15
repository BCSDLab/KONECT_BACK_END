package gg.agit.konect.domain.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClubDetailUpdateRequest(
    @Schema(description = "동아리 상세 소개", example = "BCSD에서 얻을 수 있는 경험\n1. IT 실무 경험", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "상세 소개는 필수 입력입니다.")
    String introduce,

    @Schema(description = "대표 임원진 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "대표 임원진 정보는 필수입니다.")
    @Valid
    Representative representative
) {
    public record Representative(
        @Schema(description = "대표 임원진 성명", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "성명은 필수 입력입니다.")
        @Size(max = 50, message = "성명은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "대표 임원진 전화번호", example = "01012345678", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "전화번호는 필수 입력입니다.")
        @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
        String phoneNumber,

        @Schema(description = "대표 임원진 이메일", example = "example@koreatech.ac.kr", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "이메일은 필수 입력입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
        String email
    ) {
    }
}
