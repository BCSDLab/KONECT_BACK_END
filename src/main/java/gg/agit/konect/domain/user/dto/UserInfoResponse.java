package gg.agit.konect.domain.user.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.user.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserInfoResponse(
    @Schema(description = "사용자 이름", example = "이동훈", requiredMode = REQUIRED)
    String name,

    @Schema(description = "사용자 학교 이름", example = "한국기술교육대학교", requiredMode = REQUIRED)
    String universityName,

    @Schema(description = "사용자 학번", example = "2021136091", requiredMode = REQUIRED)
    String studentNumber,

    @Schema(description = "사용자 전화번호", example = "010-1234-5678", requiredMode = NOT_REQUIRED)
    String phoneNumber
) {

    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
            user.getName(),
            user.getUniversity().getKoreanName(),
            user.getStudentNumber(),
            user.getPhoneNumber()
        );
    }
}
